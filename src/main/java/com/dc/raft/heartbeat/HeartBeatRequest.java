package com.dc.raft.heartbeat;

import com.dc.raft.command.RequestCommand;
import com.dc.raft.node.RaftPeers;
import lombok.*;

@ToString
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HeartBeatRequest extends RequestCommand {

    /**
     * leader节点
     */
    private RaftPeers.PeerNode leaderNode;

    /**
     * 当前任期
     */
    private long term;


    @Override
    public String getModule() {
        return null;
    }
}
