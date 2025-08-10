package com.myapp.neuron.net.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myapp.neuron.net.service.InMemoryCache;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private final InMemoryCache cache;
    private final ObjectMapper mapper;

    public CacheController(InMemoryCache cache, ObjectMapper mapper) {
        this.cache = cache;
        this.mapper = mapper;
    }

    /** CREATE (fails if key exists) */
    @PostMapping(value = "/{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(
            @PathVariable @NotBlank String key,
            @RequestBody String body) {

        boolean created = cache.putIfAbsent(key, body);
        if (!created) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Key already exists");
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** READ */
    @GetMapping(value = "/{key}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> read(@PathVariable @NotBlank String key) {
        String val = cache.get(key);
        if (val == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found");
        // Best-effort: if it looks like JSON, return as JSON; else return plain text
        try {
            JsonNode node = mapper.readTree(val);
            return ResponseEntity.ok(node);
        } catch (Exception ignored) {
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(val);
        }
    }

    /** REPLACE (upsert) */
    @PutMapping(value = "/{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> replace(
            @PathVariable @NotBlank String key,
            @RequestBody String body) {
        cache.put(key, body);
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH (JSON Merge Patch–style):
     * - If stored value is JSON object and body is JSON object -> shallow/deep merge fields
     * - Otherwise, treat as replace (idempotent fallback)
     */
    @PatchMapping(value = "/{key}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> patch(@PathVariable @NotBlank String key,
                                   @RequestBody String patchJson) {
        String current = cache.get(key);
        if (current == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found");

        try {
            JsonNode currentNode = mapper.readTree(current);
            JsonNode patchNode = mapper.readTree(patchJson);

            if (currentNode.isObject() && patchNode.isObject()) {
                ObjectNode merged = ((ObjectNode) currentNode).deepCopy();
                mergeObjects(merged, (ObjectNode) patchNode);
                String newVal = mapper.writeValueAsString(merged);
                cache.put(key, newVal);
                return ResponseEntity.ok(mapper.readTree(newVal));
            } else {
                // Fallback: replace entirely
                cache.put(key, patchJson);
                return ResponseEntity.ok(mapper.readTree(patchJson));
            }
        } catch (Exception e) {
            // If parsing fails, treat body as plain text and replace
            cache.put(key, patchJson);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(patchJson);
        }
    }

    /** DELETE */
    @DeleteMapping("/{key}")
    public ResponseEntity<?> delete(@PathVariable @NotBlank String key) {
        boolean removed = cache.delete(key);
        return removed ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found");
    }

    /** Recursive JSON object merge (RFC 7386-ish for objects) */
    private void mergeObjects(ObjectNode target, ObjectNode patch) {
        patch.fields().forEachRemaining(entry -> {
            String field = entry.getKey();
            JsonNode patchVal = entry.getValue();
            if (patchVal.isNull()) {
                target.remove(field); // null deletes field
            } else if (patchVal.isObject() && target.has(field) && target.get(field).isObject()) {
                mergeObjects((ObjectNode) target.get(field), (ObjectNode) patchVal);
            } else {
                target.set(field, patchVal);
            }
        });
    }
}
