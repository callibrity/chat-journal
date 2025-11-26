package com.callibrity.ai.chatjournal.example.sse;

/**
 * Wrapper record for SSE chunk data to preserve whitespace during JSON serialization.
 */
public record Chunk(String content) {
}
