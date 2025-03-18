package com.skiresort.consumer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

public class ConsumerRunnable implements Runnable {
    private final BlockingQueue<Channel> channelPool;
    private final String queueName;
    private final Gson gson = new Gson();

    public ConsumerRunnable(BlockingQueue<Channel> channelPool, String queueName) {
        this.channelPool = channelPool;
        this.queueName = queueName;
    }

    @Override
    public void run() {
        Channel channel = null;
        try {
            channel = channelPool.take();
            channel.queueDeclare(queueName, false, false, false, null);
            channel.basicQos(10);

            Channel finalChannel = channel;

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                JsonObject json = gson.fromJson(message, JsonObject.class);

                int skierID = json.get("skierID").getAsInt();
                SkierDataStore.addLiftRide(skierID, json);

                System.out.println(Thread.currentThread().getId() + " - Received: " + message);

                try {
                    finalChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (IOException e) {
                    System.err.println("Failed to process message: " + e.getMessage());
                    try {
                        finalChannel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true); // 重新入队
                    } catch (IOException ex) {
                        System.err.println("Failed to requeue message: " + ex.getMessage());
                    }
                }
            };
            ;

            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {});

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            if (channel != null) {
                try {
                    channelPool.put(channel);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}



