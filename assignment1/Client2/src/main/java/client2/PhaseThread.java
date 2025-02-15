package client2;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class PhaseThread implements Runnable {
    private final int numRequests;
    private final CountDownLatch latch;
    private final BlockingQueue<LiftRideEvent> eventQueue;
    private final SkiersApi apiInstance;
    private static final int MAX_RETRIES = 5;
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private static final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> csvRecords = Collections.synchronizedList(new ArrayList<>());

    public PhaseThread(int numRequests, CountDownLatch latch, BlockingQueue<LiftRideEvent> eventQueue) {
        this.numRequests = numRequests;
        this.latch = latch;
        this.eventQueue = eventQueue;
        this.apiInstance = new SkiersApi(new ApiClient().setBasePath("http://localhost:8080/my_lab2-1.0-SNAPSHOT"));
    }

    public void run() {
        for (int i = 0; i < numRequests; i++) {
            try {
                LiftRideEvent event = eventQueue.take();
                LiftRide liftRide = new LiftRide().liftID(event.getLiftID()).time(event.getTime());
                long startTime = System.currentTimeMillis();

                int responseCode = sendRequest(liftRide, event);
                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;

                latencies.add(latency);
                csvRecords.add(startTime + ",POST," + latency + "," + responseCode);
            } catch (InterruptedException ignored) {}
        }
        latch.countDown();
    }

    private int sendRequest(LiftRide liftRide, LiftRideEvent event) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                apiInstance.writeNewLiftRide(liftRide, event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID());
                successCount.incrementAndGet();
                return 201;
            } catch (ApiException e) {
                attempts++;
            }
        }
        failureCount.incrementAndGet();
        return 500;
    }

    public static int getSuccessCount() { return successCount.get(); }
    public static int getFailureCount() { return failureCount.get(); }
    public static List<Long> getLatencies() { return latencies; }
    public static List<String> getCsvRecords() { return csvRecords; }
}




