package com.skiresort.server;

import com.google.gson.JsonObject;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

public class MessageQueueService {
    private static final String RABBITMQ_HOST = "52.33.65.93";
    private static final String QUEUE_NAME = "ski_lift_queue";
    private static final String USERNAME = "myadmin";
    private static final String PASSWORD = "mypassword";
    private static final int NUM_CHANNELS = 10;

    private Connection connection;
    private BlockingQueue<Channel> channelPool;

    public MessageQueueService() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(RABBITMQ_HOST);
        factory.setUsername(USERNAME);
        factory.setPassword(PASSWORD);
        connection = factory.newConnection();

        channelPool = new LinkedBlockingQueue<>();
        for (int i = 0; i < NUM_CHANNELS; i++) {
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            channelPool.add(channel);
        }
    }

    public boolean sendMessage(JsonObject message) {
        Channel channel = null;
        try {
            channel = channelPool.take();
            channel.basicPublish("", QUEUE_NAME, null, message.toString().getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (channel != null) {
                channelPool.add(channel);
            }
        }
    }

    public void close() {
        try {
            for (Channel channel : channelPool) {
                channel.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}

