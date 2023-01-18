package com.dc.raft.vote;

import com.dc.raft.command.ResponseCommand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
public class VoteResponse extends ResponseCommand {

    /**
     * 当前任期号，以便于候选人去更新自己的任期
     */
    long term;

    /**
     * 候选人赢得了此张选票时为真
     */
    boolean voteGranted;

    public VoteResponse(boolean voteGranted, long term) {
        this.voteGranted = voteGranted;
        this.term = term;
    }

    public static VoteResponse fail() {
        return new VoteResponse(false, 0);
    }

    public static VoteResponse fail(long term) {
        return new VoteResponse(false, term);
    }

    public static VoteResponse ok() {
        return new VoteResponse(true, 0);
    }

    public static VoteResponse ok(long term) {
        return new VoteResponse(true, term);
    }

}
