package com.marketplace.observer;

/**
 * A source of events of family {@code E}. Observers subscribe per event type.
 */
public interface Subject<E extends Event> {
    <T extends E> Subscription subscribe(EventType<T> type, Observer<? super T> observer);
}
