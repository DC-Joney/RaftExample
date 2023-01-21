package com.dc.raft.exception;

public class RaftSerializeException extends RaftException {

    public RaftSerializeException(String message, Object... args) {
        super(message, args);
    }
}
