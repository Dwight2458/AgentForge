package io.agentforge.controlplane.messaging;

public class InvalidEventEnvelopeException extends RuntimeException {
    public InvalidEventEnvelopeException(String message) {
        super(message);
    }

    public InvalidEventEnvelopeException(String message, Throwable cause) {
        super(message, cause);
    }
}
