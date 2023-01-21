package com.dc.raft.command.vote;

import com.dc.raft.command.RequestCommand;
import com.dc.raft.node.RaftPeers;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * 用于投票的RPC
 */
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteRequest extends RequestCommand {

    /**
     * 请求选票的候选人的 Id(ip:selfPort)
     */
    RaftPeers.PeerNode candidateNode;

    /**
     * 候选人的最后日志条目的索引值
     */
    long lastLogIndex;

    /**
     * 候选人最后日志条目的任期号
     */
    long lastLogTerm;

    /**
     * 当前任期编号
     */
    long term;

    @Override
    public String getModule() {
        return null;
    }
}
