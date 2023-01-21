package com.dc.raft.command.replication;

import com.dc.raft.command.ResponseCode;
import com.dc.raft.command.ResponseCommand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString
@NoArgsConstructor
@Accessors(chain = true)
public class ReplicationResponse extends ResponseCommand {

    /**
     * 当前的任期号，用于领导人去更新自己
     */
    long term;

    /**
     * 跟随者包含了匹配上 prevLogIndex 和 prevLogTerm 的日志时为真
     */
    boolean success;

    boolean notChangeIndex;


    public ReplicationResponse(boolean success) {
        this.success = success;
    }

    public static ReplicationResponse fail(String message) {
        ReplicationResponse response = new ReplicationResponse(false);
        response.setErrorCode(ResponseCode.FAIL.getCode());
        return response;
    }

    public static ReplicationResponse notChangeIndex() {
        return new ReplicationResponse(false).setNotChangeIndex(true);
    }

    public static ReplicationResponse ok() {
        return new ReplicationResponse(true);
    }

}
