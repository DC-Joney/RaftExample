package com.dc.raft.node;

import com.dc.raft.RequestClients;
import lombok.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * 所有的 peer 节点
 */
@ToString
@EqualsAndHashCode
@Getter
@Setter
public class RaftPeers implements Serializable {

    private Set<PeerNode> peerNodes = new HashSet<>();

    private PeerNode self;

    private PeerNode leader;

    public RaftPeers(InetSocketAddress selfAddress) {
        this.self = PeerNode.create(selfAddress);
        RequestClients.addClient(self);
    }

    public RaftPeers addPeer(PeerNode peerNode) {
        //如果节点是当前节点则不添加
        if (!peerNode.equals(self)) {
            peerNodes.add(peerNode);
            RequestClients.addClient(peerNode);
        }

        return this;
    }

    /**
     * 将自己设置为leader
     */
    public RaftPeers markSelfLeader(){
        this.leader = self;
        return this;
    }

    public InetSocketAddress getSelfAddress(){
        return self.address;
    }

    /**
     * 将自己设置为leader
     */
    public RaftPeers markLeader(InetSocketAddress address){
        this.leader = PeerNode.create(address);
        return this;
    }

    /**
     * 所有节点的数量，这里要加上自己
     */
    public int nodeCount(){
        return peerNodes.size() + 1;
    }


    @EqualsAndHashCode
    @Getter
    @Setter
    @AllArgsConstructor(staticName = "create")
    public static class PeerNode {
        private InetSocketAddress address;

        @Override
        public String toString() {
            return address.toString();
        }
    }
}
