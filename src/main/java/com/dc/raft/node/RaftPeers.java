package com.dc.raft.node;

import com.dc.raft.RequestClients;
import lombok.*;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 所有的 peer 节点
 *
 * @author zhangyang
 */
@ToString
@EqualsAndHashCode
@Getter
@Setter
public class RaftPeers implements Serializable, Iterable<RaftPeers.PeerNode> {

    /**
     * 其他节点，其中也包括leader节点
     */
    private Set<PeerNode> peerNodes = new HashSet<>();

    /**
     * 当前节点
     */
    private PeerNode self;

    /**
     * leader节点
     */
    private PeerNode leader;

    public RaftPeers(InetSocketAddress selfAddress) {
        this.self = PeerNode.create(selfAddress);
        RequestClients.addClient(self);
    }

    public RaftPeers(PeerNode selfNode) {
        this.self = selfNode;
        RequestClients.addClient(self);
    }

    /**
     * 添加节点
     *
     * @param peerNode peer节点
     */
    public RaftPeers addPeer(PeerNode peerNode) {
        //如果节点是当前节点则不添加
        if (!peerNode.equals(self)) {
            peerNodes.add(peerNode);
            RequestClients.addClient(peerNode);
        }

        return this;
    }

    /**
     * 批量添加节点
     *
     * @param peerNodes 节点
     */
    public RaftPeers addPeers(Set<PeerNode> peerNodes) {
        //如果节点是当前节点则不添加
        if (!peerNodes.contains(self)) {
            peerNodes.remove(self);
        }

        peerNodes.forEach(this::addPeer);
        return this;
    }

    /**
     * 将自己设置为leader
     */
    public RaftPeers markSelfLeader() {
        this.leader = self;
        return this;
    }

    /**
     * 获取当前节点的网络地址
     */
    public InetSocketAddress getSelfAddress() {
        return self.address;
    }

    /**
     * 将特定的地址设置为leader
     */
    public RaftPeers markLeader(InetSocketAddress address) {
        this.leader = PeerNode.create(address);
        return this;
    }

    /**
     * 所有节点的数量，这里要加上自己
     */
    public int nodeCount() {
        return peerNodes.size() + 1;
    }

    @Override
    public Iterator<PeerNode> iterator() {
        return peerNodes.iterator();
    }

    /**
     * 用于表示一个Peer 节点
     */
    @EqualsAndHashCode
    @Getter
    @Setter
    @AllArgsConstructor(staticName = "create")
    @NoArgsConstructor
    public static class PeerNode {
        private InetSocketAddress address;

        @Override
        public String toString() {
            return address.toString();
        }
    }
}
