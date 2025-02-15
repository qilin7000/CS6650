package client1;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

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

    public PhaseThread(int numRequests, CountDownLatch latch, BlockingQueue<LiftRideEvent> eventQueue) {
        this.numRequests = numRequests;
        this.latch = latch;
        this.eventQueue = eventQueue;
        this.apiInstance = new SkiersApi(new ApiClient().setBasePath("http://ec2-54-214-18-127.us-west-2.compute.amazonaws.com:8080/my_lab2_war"));
    }

    public void run() {
        for (int i = 0; i < numRequests; i++) {
            try {
                LiftRideEvent event = eventQueue.take();
                LiftRide liftRide = new LiftRide().liftID(event.getLiftID()).time(event.getTime());

                if (sendRequest(liftRide, event)) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                }
            } catch (InterruptedException ignored) {}
        }
        latch.countDown();
    }


    private boolean sendRequest(LiftRide liftRide, LiftRideEvent event) {
        int attempts = 0;
        while (attempts < MAX_RETRIES) {
            try {
                apiInstance.writeNewLiftRide(liftRide, event.getResortID(), event.getSeasonID(), event.getDayID(), event.getSkierID());
                return true;  //
            } catch (ApiException e) {
                attempts++;
            }
        }
        return false;
    }

    public static int getSuccessCount() {
        return successCount.get();
    }

    public static int getFailureCount() {
        return failureCount.get();
    }
}

