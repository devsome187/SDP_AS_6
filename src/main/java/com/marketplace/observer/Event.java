package com.marketplace.observer;

/**
 * An immutable fact that happened in the marketplace. Every event knows its type,
 * which is the key listeners subscribe by.
 */
public interface Event {
    EventType<?> type();
}
