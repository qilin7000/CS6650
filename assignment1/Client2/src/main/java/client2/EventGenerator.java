package client2;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class EventGenerator implements Runnable {
    private final int numEvents;
    private final BlockingQueue<LiftRideEvent> eventQueue;

    public EventGenerator(int numEvents, BlockingQueue<LiftRideEvent> eventQueue) {
        this.numEvents = numEvents;
        this.eventQueue = eventQueue;
    }

    @Override
    public void run() {
        for (int i = 0; i < numEvents; i++) {
            int skierID = ThreadLocalRandom.current().nextInt(1, 100001);
            int resortID = ThreadLocalRandom.current().nextInt(1, 11);
            int liftID = ThreadLocalRandom.current().nextInt(1, 41);
            int time = ThreadLocalRandom.current().nextInt(1, 361);
            String seasonID = "2025";
            String dayID = "1";

            LiftRideEvent event = new LiftRideEvent(skierID, resortID, liftID, seasonID, dayID, time);
            try {
                eventQueue.put(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}