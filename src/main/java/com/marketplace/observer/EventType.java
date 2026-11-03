package com.marketplace.observer;

/**
 * Type-safe subscription key. The type parameter ties the key to the event class it
 * identifies, so {@code subscribe(PriceChanged.TYPE, observer)} only compiles for an
 * observer that accepts {@code PriceChanged}. Instances are compared by identity and
 * are meant to be declared once as constants on the event class.
 */
public final class EventType<E extends Event> {
    private final String name;

    private EventType(String name) {
        this.name = name;
    }

    public static <E extends Event> EventType<E> named(String name) {
        return new EventType<>(name);
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
