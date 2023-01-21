package com.dc.raft;

import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;

import java.time.Duration;

public interface Requester {


    <T extends ResponseCommand> T request(RequestCommand request);


    <T extends ResponseCommand> T request(RequestCommand request, Duration timeout);


}
