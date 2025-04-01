package com.skiresort.consumer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.rabbitmq.client.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public class ConsumerRunnable implements Runnable {
    private static final String TABLE_NAME = "lift_rides";
    private static final int BATCH_SIZE = 25;
    private static final int FLUSH_INTERVAL_MS = 100;

    private final BlockingQueue<Channel> channelPool;
    private final String queueName;
    private final Gson gson = new Gson();
    private final DynamoDbClient dynamoDbClient;
    private final List<Map<String, AttributeValue>> localBatch = new ArrayList<>();
    private long lastFlushTime = System.currentTimeMillis();

    public ConsumerRunnable(BlockingQueue<Channel> channelPool, String queueName) {
        this.channelPool = channelPool;
        this.queueName = queueName;
        this.dynamoDbClient = DynamoDbClient.builder()
                .region(Region.US_WEST_2)
                .build();
    }

    @Override
    public void run() {
        Channel channel = null;
        try {
            channel = channelPool.take();
            channel.basicQos(10);

            Channel finalChannel = channel;
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                try {
                    JsonObject json = gson.fromJson(message, JsonObject.class);
                    addToBatch(json);
                    finalChannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                } catch (Exception e) {
                    System.err.println("Error inserting to DynamoDB: " + e.getMessage());
                    try {
                        finalChannel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, true);
                    } catch (IOException ex) {
                        System.err.println("Failed to requeue message: " + ex.getMessage());
                    }
                }
            };

            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {});

            while (true) {
                if (System.currentTimeMillis() - lastFlushTime >= FLUSH_INTERVAL_MS) {
                    flushBatch();
                    lastFlushTime = System.currentTimeMillis();
                }
                Thread.sleep(10);
            }

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

    private void addToBatch(JsonObject json) {
        try {
            String[] requiredFields = {"skierID", "time", "liftID", "resortID", "dayID", "seasonID"};
            for (String field : requiredFields) {
                if (!json.has(field) || json.get(field).isJsonNull()) {
                    System.err.println("Missing or null field: " + field + " in JSON: " + json);
                    return;
                }
            }

            int skierID = json.get("skierID").getAsInt();
            int time = json.get("time").getAsInt();
            int liftID = json.get("liftID").getAsInt();
            int resortID = json.get("resortID").getAsInt();
            int dayID = Integer.parseInt(json.get("dayID").getAsString());
            int seasonID = Integer.parseInt(json.get("seasonID").getAsString());

            if (skierID <= 0 || skierID > 100000) {
                System.err.println("Invalid skierID: " + skierID);
                return;
            }
            if (time < 0 || time > 360) {
                System.err.println("Invalid time: " + time);
                return;
            }
            if (liftID <= 0 || liftID > 60) {
                System.err.println("Invalid liftID: " + liftID);
                return;
            }
            if (resortID <= 0 || resortID > 10000) {
                System.err.println("Invalid resortID: " + resortID);
                return;
            }
            if (dayID < 1 || dayID > 366) {
                System.err.println("Invalid dayID: " + dayID);
                return;
            }
            if (seasonID < 2000 || seasonID > 2100) {
                System.err.println("Invalid seasonID: " + seasonID);
                return;
            }

            Map<String, AttributeValue> item = new HashMap<>();
            item.put("skierID", AttributeValue.builder().n(String.valueOf(skierID)).build());
            item.put("time", AttributeValue.builder().n(String.valueOf(time)).build());
            item.put("liftID", AttributeValue.builder().n(String.valueOf(liftID)).build());
            item.put("resortID", AttributeValue.builder().n(String.valueOf(resortID)).build());
            item.put("dayID", AttributeValue.builder().n(String.valueOf(dayID)).build());
            item.put("seasonID", AttributeValue.builder().n(String.valueOf(seasonID)).build());

            localBatch.add(item);

            if (localBatch.size() >= BATCH_SIZE) {
                flushBatch();
            }

        } catch (Exception e) {
            System.err.println("Failed to add item to batch: " + e.getMessage());
        }
    }


    private void flushBatch() {
        if (localBatch.isEmpty()) return;

        List<WriteRequest> writeRequests = new ArrayList<>();
        for (Map<String, AttributeValue> item : localBatch) {
            PutRequest putRequest = PutRequest.builder().item(item).build();
            writeRequests.add(WriteRequest.builder().putRequest(putRequest).build());
        }

        Map<String, List<WriteRequest>> requestItems = new HashMap<>();
        requestItems.put(TABLE_NAME, writeRequests);

        try {
            BatchWriteItemRequest batchRequest = BatchWriteItemRequest.builder()
                    .requestItems(requestItems)
                    .build();
            BatchWriteItemResponse response = dynamoDbClient.batchWriteItem(batchRequest);

            while (!response.unprocessedItems().isEmpty()) {
                response = dynamoDbClient.batchWriteItem(BatchWriteItemRequest.builder()
                        .requestItems(response.unprocessedItems())
                        .build());
            }
        } catch (Exception e) {
            System.err.println("Error during flushBatch: " + e.getMessage());
        } finally {
            localBatch.clear();
        }
    }
}





