package com.dc.raft;

import com.dc.raft.node.RaftNode;
import com.dc.raft.node.RaftPeers;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class RaftApplicationRunner implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Set<RaftPeers.PeerNode> peerNodes = Sets.newHashSet();
        for (int port = 8080; port < 8083; port++) {
            peerNodes.add(RaftPeers.PeerNode.create(new InetSocketAddress("127.0.0.1", port)));
        }

        ArrayList<ServerBootStrap> bootStraps = Lists.newArrayList();

        for (RaftPeers.PeerNode peerNode : peerNodes) {
            ServerBootStrap bootStrap = new ServerBootStrap(peerNode, peerNodes);
            bootStraps.add(bootStrap);
            bootStrap.start();
        }


        startLeaderPause(bootStraps);


    }

    private void startLeaderPause(ArrayList<ServerBootStrap> bootStraps) {

        AtomicReference<RaftPeers> pauseNode = new AtomicReference<>();

        //模拟leader节点宕机
        Executors.newScheduledThreadPool(1)
                .schedule(() -> {

                    try {

//                        RaftPeers peers = pauseNode.get();
//                        if (peers != null) {
//                            new ServerBootStrap(peers.getSelf(), peers.getPeerNodes()).start();
//                        }

                        ServerBootStrap bootStrap = bootStraps.get(0);
                        RaftPeers raftPeers = bootStrap.getRaftNode().getRaftPeers();
                        RaftPeers.PeerNode leader = raftPeers.getLeader();

                        for (ServerBootStrap strap : bootStraps) {
                            raftPeers = strap.getRaftNode().getRaftPeers();
                            if (leader.equals(raftPeers.getSelf())) {
                                strap.stop();
                                pauseNode.set(raftPeers);
                                log.info("Leader pause: {}", raftPeers.getSelf());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Raft node start fail, node is: {}, cause is: ", pauseNode.get().getSelf(), e);
                    }
                }, 30,  TimeUnit.SECONDS);
    }
}
