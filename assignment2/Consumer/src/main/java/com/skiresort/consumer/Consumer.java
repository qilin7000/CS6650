package com.skiresort.consumer;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.*;

public class Consumer {
    private static final String RABBITMQ_HOST = "52.33.65.93";
    private static final String QUEUE_NAME = "ski_lift_queue";
    private static final String USERNAME = "myadmin";
    private static final String PASSWORD = "mypassword";
    private static final int NUM_CONSUMER_THREADS = 100;
    private static final int NUM_CHANNELS = 20;

    private static BlockingQueue<Channel> channelPool;

    public static void main(String[] args) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);

        try {
            Connection connection = factory.newConnection();
            channelPool = new LinkedBlockingQueue<>();

            for (int i = 0; i < NUM_CHANNELS; i++) {
                Channel channel = connection.createChannel();
                channel.queueDeclare(QUEUE_NAME, false, false, false, null);
                channel.basicQos(10);
                channelPool.add(channel);
            }

            System.out.println("Consumer started. Waiting for messages...");
            ExecutorService threadPool = Executors.newFixedThreadPool(NUM_CONSUMER_THREADS);

            for (int i = 0; i < NUM_CONSUMER_THREADS; i++) {
                threadPool.submit(new ConsumerRunnable(channelPool, QUEUE_NAME));
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                threadPool.shutdown();
                try {
                    for (Channel ch : channelPool) {
                        ch.close();
                    }
                    connection.close();
                    System.out.println("Consumer shutting down.");
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

