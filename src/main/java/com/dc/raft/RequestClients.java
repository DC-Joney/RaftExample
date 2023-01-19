package com.dc.raft;

import com.dc.raft.node.RaftPeers;
import com.dc.raft.rpc.GrpcClient;
import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Map;

public class RequestClients {

    private static final Map<RaftPeers.PeerNode, GrpcClient> requestClients = new HashMap<>();

    public static synchronized void addClient(RaftPeers.PeerNode peerNode) {
        GrpcClient grpcClient = new GrpcClient(peerNode.getAddress());
        grpcClient.start();
        requestClients.putIfAbsent(peerNode, grpcClient);
    }

    @NonNull
    public static GrpcClient getClient(RaftPeers.PeerNode peerNode) {
        return requestClients.get(peerNode);
    }

}
