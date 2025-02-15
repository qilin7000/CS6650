package client1;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.LiftRide;

import java.util.concurrent.*;

public class Client {
    private static final int NUM_TOTAL_REQUESTS = 200000;
    private static final int NUM_INITIAL_THREADS = 32;
    private static final int REQUESTS_PER_INITIAL_THREAD = 1000;
    private static final BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(NUM_TOTAL_REQUESTS);

    public static void main(String[] args) throws InterruptedException {

        testLatency();

        long startTime = System.currentTimeMillis();

        Thread eventGenerator = new Thread(new EventGenerator(NUM_TOTAL_REQUESTS, eventQueue));
        eventGenerator.start();
        eventGenerator.join();


        ExecutorService threadPool = Executors.newFixedThreadPool(NUM_INITIAL_THREADS);
        CountDownLatch latch1 = new CountDownLatch(NUM_INITIAL_THREADS);
        for (int i = 0; i < NUM_INITIAL_THREADS; i++) {
            threadPool.execute(new PhaseThread(REQUESTS_PER_INITIAL_THREAD, latch1, eventQueue));
        }
        latch1.await();


        int remainingRequests = NUM_TOTAL_REQUESTS - (NUM_INITIAL_THREADS * REQUESTS_PER_INITIAL_THREAD);
        int numAdditionalThreads = Math.max(1, remainingRequests / 1000); // Determine number of threads based on tasks
        CountDownLatch latch2 = new CountDownLatch(numAdditionalThreads);
        for (int i = 0; i < numAdditionalThreads; i++) {
            threadPool.execute(new PhaseThread(remainingRequests / numAdditionalThreads, latch2, eventQueue));
        }
        latch2.await();
        threadPool.shutdown();


        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        int totalRequests = PhaseThread.getSuccessCount() + PhaseThread.getFailureCount();
        double actualThroughput = totalRequests / (totalTime / 1000.0);

        System.out.println("Phase one threads: " + NUM_INITIAL_THREADS);
        System.out.println("Phase two threads: " + numAdditionalThreads);
        System.out.println("All requests completed. Total execution time: " + totalTime + " ms");
        System.out.println("Successful requests: " + PhaseThread.getSuccessCount());
        System.out.println("Failed requests: " + PhaseThread.getFailureCount());
        System.out.println("Throughput: " + actualThroughput);

    }
    private static void testLatency() {
        SkiersApi apiInstance = new SkiersApi(new ApiClient().setBasePath("http://ec2-54-214-18-127.us-west-2.compute.amazonaws.com:8080/my_lab2_war"));
        int testRequests = 10000;
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < testRequests; i++) {
            int skierID = ThreadLocalRandom.current().nextInt(1, 100001);
            int liftID = ThreadLocalRandom.current().nextInt(1, 41);
            int time = ThreadLocalRandom.current().nextInt(1, 361);
            int resortID = ThreadLocalRandom.current().nextInt(1, 10);
            LiftRide liftRide = new LiftRide().liftID(liftID).time(time);

            try {
                apiInstance.writeNewLiftRide(liftRide, resortID, "2025", "1", skierID);
            } catch (ApiException e) {
                System.err.println("Request failed: " + e.getMessage());
            }
        }

        long testEndTime = System.currentTimeMillis();
        double totalTimeToSec = (testEndTime - testStartTime) / 1000.0;
        System.out.println("Single-threaded average request latency: " + (totalTimeToSec / testRequests) * 10000 + " ms");
        System.out.println("Total request: " + testRequests);
    }



  }





