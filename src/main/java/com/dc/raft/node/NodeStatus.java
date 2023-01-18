package com.dc.raft.node;

public enum NodeStatus {

    FOLLOWER(0), CANDIDATE(1), LEADER(2);

    NodeStatus(int code) {
        this.code = code;
    }

    int code;

    public static NodeStatus value(int i) {
        for (NodeStatus value : values()) {
            if (value.code == i) {
                return value;
            }
        }
        return null;
    }
}
