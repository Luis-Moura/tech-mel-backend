package com.tech_mel.tech_mel.application.exception;

public class RateLimitExceededException extends RuntimeException {
    private final long waitTimeSeconds;

    public RateLimitExceededException(long waitTimeSeconds) {
        super("Limite de requisições excedido. Tente novamente mais tarde.");
        this.waitTimeSeconds = waitTimeSeconds;
    }

    public long getWaitTimeSeconds() {
        return waitTimeSeconds;
    }
}