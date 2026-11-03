package com.marketplace.observer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe listener registry shared by all subjects through composition.
 *
 * <p>Each event type has its own copy-on-write list, so there is no lock on the whole
 * subject: publishers of different types never contend, and publishing iterates over a
 * snapshot. A listener that subscribes or cancels while an event is being delivered
 * (even from inside its own handler) takes effect from the next event on.
 */
public final class ObserverRegistry<E extends Event> implements Subject<E> {
    private final Map<EventType<?>, List<Registration<?>>> registrations = new ConcurrentHashMap<>();

    @Override
    public <T extends E> Subscription subscribe(EventType<T> type, Observer<? super T> observer) {
        Registration<T> registration = new Registration<>(observer);
        List<Registration<?>> forType = registrations.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>());
        forType.add(registration);
        return () -> forType.remove(registration);
    }

    /**
     * Delivers the event to every observer of its type. An observer that throws does not
     * stop delivery to the others: the first failure is rethrown once everyone has been
     * notified, with any later failures attached as suppressed exceptions.
     */
    public void publish(E event) {
        List<Registration<?>> forType = registrations.get(event.type());
        if (forType == null) {
            return;
        }
        RuntimeException failure = null;
        for (Registration<?> registration : forType) {
            try {
                deliver(registration, event);
            } catch (RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public int observerCount(EventType<? extends E> type) {
        List<Registration<?>> forType = registrations.get(type);
        return forType == null ? 0 : forType.size();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> void deliver(Registration<T> registration, Event event) {
        // Safe: a registration is stored only under the EventType<T> it was created for,
        // and an event's type() always matches its own class.
        registration.observer.onEvent((T) event);
    }

    /** Identity wrapper, so the same observer can be subscribed twice and cancelled independently. */
    private static final class Registration<T extends Event> {
        private final Observer<? super T> observer;

        private Registration(Observer<? super T> observer) {
            this.observer = observer;
        }
    }
}
