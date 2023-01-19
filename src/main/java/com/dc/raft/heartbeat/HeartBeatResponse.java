package com.dc.raft.heartbeat;

import com.dc.raft.command.ResponseCommand;
import lombok.*;

@ToString
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HeartBeatResponse extends ResponseCommand {

    private long term;

    /**
     * 发送心跳是否成功
     */
    private boolean heartSuccess;


    public static HeartBeatResponse success(long term) {
        return new HeartBeatResponse(term, true);
    }

    public static HeartBeatResponse fail(long term) {
        return new HeartBeatResponse(term, false);
    }


}
