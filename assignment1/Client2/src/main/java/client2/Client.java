package client2;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class Client {
    private static final int NUM_TOTAL_REQUESTS = 200000;
    private static final int NUM_INITIAL_THREADS = 32;
    private static final int REQUESTS_PER_INITIAL_THREAD = 1000;
    private static final BlockingQueue<LiftRideEvent> eventQueue = new LinkedBlockingQueue<>(NUM_TOTAL_REQUESTS);

    public static void main(String[] args) throws InterruptedException {

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
        int numAdditionalThreads = Math.max(1, remainingRequests / 1000);
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

        writeCsv(PhaseThread.getCsvRecords());
        computeStatistics(PhaseThread.getLatencies(), totalTime, totalRequests);
    }

    private static void computeStatistics(List<Long> latencies, long totalTime, int totalRequests) {
        if (latencies.isEmpty()) {
            System.out.println("No latency data collected.");
            return;
        }

        Collections.sort(latencies);
        long minLatency = latencies.get(0);
        long maxLatency = latencies.get(latencies.size() - 1);
        double meanLatency = latencies.stream().mapToDouble(Long::doubleValue).average().orElse(0);
        long medianLatency = latencies.get(latencies.size() / 2);
        long p99Latency = latencies.get((int) (latencies.size() * 0.99));

        double throughput = totalRequests / (totalTime / 1000.0);

        System.out.println("Throughput: " + throughput + " requests/sec");
        System.out.println("Min latency: " + minLatency + " ms");
        System.out.println("Max latency: " + maxLatency + " ms");
        System.out.println("Mean latency: " + meanLatency + " ms");
        System.out.println("Median latency: " + medianLatency + " ms");
        System.out.println("P99 latency: " + p99Latency + " ms");
    }


    private static void writeCsv(List<String> csvRecords) {
        String csvFile = "request_log.csv";
        try (FileWriter writer = new FileWriter(csvFile)) {
            writer.write("StartTime,RequestType,Latency,ResponseCode\n");
            for (String record : csvRecords) {
                writer.write(record + "\n");
            }
            System.out.println("CSV log written to: " + csvFile);
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }
}




