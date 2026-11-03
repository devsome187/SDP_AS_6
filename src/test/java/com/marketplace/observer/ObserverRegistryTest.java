package com.marketplace.observer;

import com.marketplace.event.PriceChanged;
import com.marketplace.event.StockChanged;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObserverRegistryTest {
    private final ObserverRegistry<Event> registry = new ObserverRegistry<>();

    private static PriceChanged priceEvent(long version) {
        return new PriceChanged("X", BigDecimal.TEN, BigDecimal.ONE, version);
    }

    @Test
    void deliversOnlyToObserversOfTheEventsType() {
        List<Event> prices = new ArrayList<>();
        List<Event> stock = new ArrayList<>();
        registry.subscribe(PriceChanged.TYPE, prices::add);
        registry.subscribe(StockChanged.TYPE, stock::add);

        registry.publish(priceEvent(1));

        assertEquals(1, prices.size());
        assertTrue(stock.isEmpty());
    }

    @Test
    void cancelledSubscriptionReceivesNothingMore() {
        List<Event> received = new ArrayList<>();
        Subscription subscription = registry.subscribe(PriceChanged.TYPE, received::add);

        registry.publish(priceEvent(1));
        subscription.cancel();
        subscription.cancel();
        registry.publish(priceEvent(2));

        assertEquals(1, received.size());
        assertEquals(0, registry.observerCount(PriceChanged.TYPE));
    }

    @Test
    void observerCanUnsubscribeFromInsideItsOwnHandler() {
        List<Long> received = new ArrayList<>();
        List<Long> neighbour = new ArrayList<>();
        AtomicReference<Subscription> self = new AtomicReference<>();
        self.set(registry.subscribe(PriceChanged.TYPE, event -> {
            received.add(event.version());
            self.get().cancel();
        }));
        registry.subscribe(PriceChanged.TYPE, event -> neighbour.add(event.version()));

        registry.publish(priceEvent(1));
        registry.publish(priceEvent(2));

        assertEquals(List.of(1L), received);
        assertEquals(List.of(1L, 2L), neighbour, "other observers are not affected by the removal");
    }

    @Test
    void observerSubscribedFromAHandlerStartsWithTheNextEvent() {
        List<Long> late = new ArrayList<>();
        Subscription[] once = new Subscription[1];
        once[0] = registry.subscribe(PriceChanged.TYPE, event -> {
            registry.subscribe(PriceChanged.TYPE, joined -> late.add(joined.version()));
            once[0].cancel();
        });

        registry.publish(priceEvent(1));
        registry.publish(priceEvent(2));

        assertEquals(List.of(2L), late);
    }

    @Test
    void sameObserverSubscribedTwiceIsCancelledIndependently() {
        List<Event> received = new ArrayList<>();
        Observer<Event> observer = received::add;
        Subscription first = registry.subscribe(PriceChanged.TYPE, observer);
        registry.subscribe(PriceChanged.TYPE, observer);

        first.cancel();
        registry.publish(priceEvent(1));

        assertEquals(1, received.size());
    }

    @Test
    void subscribingAndCancellingWhileOtherThreadsPublishIsSafe() throws Exception {
        List<Event> stable = new CopyOnWriteArrayList<>();
        registry.subscribe(PriceChanged.TYPE, stable::add);
        int publishers = 3;
        int eventsEach = 2_000;
        ExecutorService executor = Executors.newFixedThreadPool(publishers + 1);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int p = 0; p < publishers; p++) {
            futures.add(executor.submit(() -> {
                start.await();
                for (int i = 0; i < eventsEach; i++) {
                    registry.publish(priceEvent(i));
                }
                return null;
            }));
        }
        futures.add(executor.submit(() -> {
            start.await();
            for (int i = 0; i < eventsEach; i++) {
                registry.subscribe(PriceChanged.TYPE, event -> { }).cancel();
            }
            return null;
        }));

        start.countDown();
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(publishers * eventsEach, stable.size());
        assertEquals(1, registry.observerCount(PriceChanged.TYPE));
    }
}
