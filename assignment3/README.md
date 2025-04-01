# CS6650 Assignment 2 

This project implements a distributed system for processing ski lift ride events, utilizing a multi-threaded client, a servlet-based server, and a RabbitMQ-based message queue.

## Overview
The system consists of:
- **Client**: Sends lift ride events to the server in multiple phases to optimize throughput.
- **Server**: Validates requests and publishes messages to RabbitMQ.
- **Message Queue (RabbitMQ)**: Ensures asynchronous processing of messages.
- **Consumer**: Retrieves messages from the queue and processes them.

## System Architecture
- **Client** (Runs locally)
    - Sends 200,000 requests in a staged manner (first phase with 32 threads, each sending 1,000 requests).
    - Measures throughput and response times.

- **Server** (Runs on AWS EC2 - Amazon Linux)
    - Handles HTTP POST requests.
    - Validates URL parameters and JSON payloads.
    - Pushes valid requests to RabbitMQ.

- **RabbitMQ** (Runs on AWS EC2 - Ubuntu Linux)
    - Manages queueing of lift ride events.

- **Consumer** (Runs on AWS EC2 - Amazon Linux)
    - Reads messages from the queue and processes them concurrently.
    - Stores skier ride data in a thread-safe in-memory data structure.

### Prerequisites
- Java 17 (Corretto)
- Apache Tomcat 9+
- RabbitMQ 3.12+
- Maven
- Git