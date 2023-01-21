package com.dc.raft.command.replication;

import com.dc.raft.command.RequestCommand;
import com.dc.raft.node.RaftPeers;
import com.dc.raft.store.LogEntry;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Getter
@Setter
@ToString
@Accessors(chain = true)
public class ReplicationRequest extends RequestCommand {

    private long term;

    private RaftPeers.PeerNode leader;

    /**
     * leader节点 commit的位置
     */
    private long leaderCommit;

    /**
     * leader节点最后一条日志的前一条日志
     */
    private long prevTerm = -1;

    private long prevIndex = -1;


    /**
     * 要被复制的log日志
     */
    private List<LogEntry> logEntries;

    public boolean isEmptyEntries() {
        return CollectionUtils.isEmpty(logEntries);
    }


    public LogEntry getFirstEntry() {
        return logEntries.get(0);
    }

    public LogEntry getLastEntry() {
        return logEntries.get(logEntries.size() - 1);
    }

    @Override
    public String getModule() {
        return null;
    }
}
