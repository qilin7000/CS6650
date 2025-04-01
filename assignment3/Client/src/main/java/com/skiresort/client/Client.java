package com.skiresort.client;

import io.swagger.client.ApiClient;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
    private static final int NUM_INITIAL_THREADS = 32;
    private static final int REQUESTS_PER_THREAD = 1000;
    private static final int TOTAL_REQUESTS = 200000;
    private static final int MAX_RETRIES = 5;

    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failureCount = new AtomicInteger(0);
    private static final BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(TOTAL_REQUESTS);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ApiClient apiClient = new ApiClient();
        apiClient.setBasePath("http://ec2-44-239-178-225.us-west-2.compute.amazonaws.com:8080/QLServer-1.0-SNAPSHOT");

        long startTime = System.currentTimeMillis();

        Thread eventGenerator = new Thread(new EventGenerator(TOTAL_REQUESTS, eventQueue));
        eventGenerator.start();
        eventGenerator.join();

        ExecutorService threadPool = Executors.newFixedThreadPool(NUM_INITIAL_THREADS);
        CompletionService<Void> completionService = new ExecutorCompletionService<>(threadPool);

        int remainingRequests = TOTAL_REQUESTS;
        int activeThreads = 0;

        while (remainingRequests > 0 || activeThreads > 0) {
            while (activeThreads < NUM_INITIAL_THREADS && remainingRequests > 0) {
                int batchSize = Math.min(REQUESTS_PER_THREAD, remainingRequests);
                completionService.submit(new PhaseThread(batchSize, eventQueue, apiClient, successCount, failureCount));
                remainingRequests -= batchSize;
                activeThreads++;
            }

            Future<Void> future = completionService.take();
            future.get();
            activeThreads--;
        }

        threadPool.shutdown();
        threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        double throughput = (double) TOTAL_REQUESTS / (totalTime / 1000.0);

        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + failureCount.get());
        System.out.println("Total execution time: " + totalTime + " ms");
        System.out.println("Throughput: " + throughput);
    }
}

















