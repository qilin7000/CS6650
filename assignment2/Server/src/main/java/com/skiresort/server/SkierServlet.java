package com.skiresort.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/skiers/*")
public class SkierServlet extends HttpServlet {
    private MessageQueueService messageQueueService;
    private final Gson gson = new Gson();

    @Override
    public void init() throws ServletException {
        try {
            messageQueueService = new MessageQueueService();
            System.out.println("RabbitMQ connection initialized.");
        } catch (Exception e) {
            throw new ServletException("Failed to initialize RabbitMQ", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");

        StringBuilder jsonBody = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBody.append(line);
            }
        }

        String urlPath = req.getPathInfo();
        if (!RequestValidator.isUrlValid(urlPath)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"error\": \"Invalid URL format\"}");
            return;
        }

        JsonObject jsonMessage;
        try {
            jsonMessage = gson.fromJson(jsonBody.toString(), JsonObject.class);
        } catch (JsonSyntaxException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"error\": \"Invalid JSON format\"}");
            return;
        }

        if (!RequestValidator.isPayloadValid(jsonMessage)) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getWriter().write("{\"error\": \"Invalid JSON payload\"}");
            return;
        }

        jsonMessage.addProperty("skierID", RequestValidator.extractSkierID(urlPath));
        boolean success = messageQueueService.sendMessage(jsonMessage);

        if (success) {
            res.setStatus(HttpServletResponse.SC_CREATED);
            res.getWriter().write("{\"message\": \"Data sent to queue\"}");
        } else {
            res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            res.getWriter().write("{\"error\": \"Failed to send message to queue\"}");
        }
    }

    @Override
    public void destroy() {
        messageQueueService.close();
        System.out.println("Server shutting down. Closed RabbitMQ connections.");
    }
}

