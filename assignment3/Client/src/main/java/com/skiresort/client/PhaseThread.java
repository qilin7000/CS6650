package com.skiresort.client;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class PhaseThread implements Callable<Void> {
    private final int numRequests;
    private final BlockingQueue<LiftRideEvent> eventQueue;
    private final SkiersApi apiInstance;
    private static final int MAX_RETRIES = 5;
    private final AtomicInteger successCount;
    private final AtomicInteger failureCount;

    public PhaseThread(int numRequests, BlockingQueue<LiftRideEvent> eventQueue, ApiClient apiClient, AtomicInteger successCount, AtomicInteger failureCount) {
        this.numRequests = numRequests;
        this.eventQueue = eventQueue;
        this.apiInstance = new SkiersApi(apiClient);
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    @Override
    public Void call() {
        for (int i = 0; i < numRequests; i++) {
            try {
                LiftRideEvent event = eventQueue.take();
                LiftRide liftRide = new LiftRide().liftID(event.getLiftID()).time(event.getTime());

                if (sendRequestWithRetries(liftRide, event)) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
            } catch (InterruptedException ignored) {}
        }
        return null;
    }

    private boolean sendRequestWithRetries(LiftRide liftRide, LiftRideEvent event) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                apiInstance.writeNewLiftRide(liftRide, event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID());
                return true;
            } catch (ApiException e) {
                attempts++;
            }
        }
        return false;
    }
}

