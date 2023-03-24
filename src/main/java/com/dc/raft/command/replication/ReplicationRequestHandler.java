package com.dc.raft.command.replication;

import com.dc.raft.annotation.RpcHandler;
import com.dc.raft.command.RequestCommand;
import com.dc.raft.command.ResponseCommand;
import com.dc.raft.handler.RequestHandler;
import com.dc.raft.node.NodeStatus;
import com.dc.raft.node.RaftNode;
import com.dc.raft.store.LogEntry;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用于处理leader复制请求
 *
 * @author zhangyang
 */
@Slf4j
@RpcHandler
public class ReplicationRequestHandler implements RequestHandler {

    private RaftNode raftNode;

    private AtomicBoolean appendLock = new AtomicBoolean(false);

    public ReplicationRequestHandler(RaftNode raftNode) {
        this.raftNode = raftNode;
    }

    @Override
    public ResponseCommand handle(RequestCommand requestCommand) {
        ReplicationRequest request = (ReplicationRequest) requestCommand;
        if (!appendLock.compareAndSet(false, true)) {
            return ReplicationResponse.notChangeIndex();
        }

        try {
            ReplicationResponse response = new ReplicationResponse();
            response.setTerm(raftNode.getTerm());

            //如果请求节点的term < 当前节点的term，则被认为请求节点可能是在网络分区之后或者是其他原因导致的滞后
            if (request.getTerm() < raftNode.getTerm()) {
                return response;
            }

            //将当前raft节点的leader节点设置为请求节点
            raftNode.setLeader(request.getLeader()).resetElectionTimeout();
            //如果请求节点的term >= 当前节点的term，则将当前节点的status 设置为follower状态
            if (request.getTerm() >= raftNode.getTerm()) {
                raftNode.markStatus(NodeStatus.FOLLOWER);
            }

            raftNode.setTerm(request.getTerm());

            /*
                再来看看日志接收者的实现步骤：
		           1、和心跳一样，要先检查对方 term，如果 term 都不对，那么就没什么好说的了。
		           2、 如果日志不匹配，那么返回 leader，告诉他，减小 nextIndex 重试。
		           3、如果本地存在的日志和 leader 的日志冲突了，以 leader 的为准，删除自身的。
		           4、最后，将日志应用到状态机，更新本地的 commitIndex，返回 leader 成功。


		       举个例子如下：


                  prevIndex = 3，logEntries: [5]，nextIndex = 4

                  leader:   2 | 3 | 3 | 4 | 5 (new)
                  follower: 2 | 3 | 3 | 5 | 7

                  当follower 接收到leader的replication请求时，会经历以下是三个步骤：

                  经历步骤1:
                    首先发现leader logIndex = 3 的logIndex 与 follower logIndex = 3的数据是不同的，那么会直接返回false
                    leader 在接收到follower的返回请求时，会将nextIndex递减，然后继续replication, 数据变为如下：
                    nextIndex = 3，prevIndex = 2，logEntries: [4, 5]

                    继续重复步骤1, 发现leader index=3 的日志与follower index=3的日志是一致的，接下来进入步骤2

                  经历步骤2：
                    follower 获取 index = prevIndex + 1 的数据，这时 prevIndex = 2
                    将follower index = 3 的数据与logEntries 中的第一条数据比较，发现不一致:
                    将follower从 index=3的数据开始进行删除，这时follower的数据变为如下：

                    leader:   2 | 3 | 3 | 4 | 5
                    follower: 2 | 3 | 3


                  步骤3：
                    将logEntries的数据写入到follower中，数据如下：

                    leader:   2 | 3 | 3 | 4 | 5
                    follower: 2 | 3 | 3 | 4 | 5



		    */
            //获取当前节点的最后一条日志索引
            Long lastIndex = raftNode.getLogStore().getLastIndex();

            // 步骤1
            //如果不是第一条数据，并且如果请求节点传入的prevIndex 与 当前节点的prevLog不匹配，则认为数据不一致
            //需要减少nextIndex重试
            //lastIndex == -1 表示当前节点还没有写入任何数据
            //request.getPrevIndex() == -1 表示这是leader节点写入的第一条数据
            if (lastIndex != -1 && request.getPrevIndex() != -1) {
                LogEntry prevLog = raftNode.getLogStore().read(request.getPrevIndex());
                // 如果日志在 prevLogIndex 位置处的日志条目的任期号和 prevLogTerm 不匹配，则返回 false
                // 需要减小 nextIndex 重试.
                if (prevLog == null)
                    return response;

                //当logIndex一致，但是term不一致时，可能是由于leader节点宕机，其他leader节点写入数据又宕机的情况
                //todo：如果一条已存在的日志与新的冲突(index相同但是term不同), 则删除已经存在的日志条目和他之后所有的日志条目
                if (prevLog.getTerm() != request.getPrevTerm())
                    return response;
            }

            //步骤2
            //上面已经保证了leader节点的prevLog 与 follower节点的prevLog 是一致的
            // 判断需要添加的日志数据是否与本地的日志数据一致，既(prevIndex + 1) - (request.getLastEntry()) 是需要被添加到follower的
            LogEntry existLog = raftNode.getLogStore().read(((request.getPrevIndex() + 1)));

            //如果当前节点的(prevIndex + 1) 的term 与 请求节点的 (prevIndex + 1) 的term不同，则需要将当前节点从(prevIndex+1)-lastIndex的数据信息删除
            if (existLog != null && existLog.getTerm() != request.getFirstEntry().getTerm()) {
                // 删除这一条和之后所有的, 然后写入日志和状态机.
                raftNode.getLogStore().truncate(request.getPrevIndex());
            }
            //如果任期相同则认为他们的日志都是相同的直接返回, 这里针对的是已经添加过的数据才会出现existLog != null, 并且term相同的情况
            else if (existLog != null) {
                // 已经有日志了, 不能重复写入.
                response.setSuccess(true);
                return response;
            }


            // 步骤3
            // 写进日志并且应用到状态机
            log.info("[{}] 将数据写入本地数据库和状态机中 ", raftNode.getSelfPeer());
            for (LogEntry entry : request.getLogEntries()) {
                raftNode.getLogStore().write(entry);

                //将写入的数据应用到状态机
                //todo: 这里是否应该只是预习，而等到leader节点确认1/2节点写入成功后再应用到状态机
                raftNode.getStateMachine().apply(entry);
                response.setSuccess(true);
            }

            //如果leader commit > 当前节点commitIndex位置，则将当前节点的commitIndex设置为leader的commitIndex
            //因为当流程走到这里时，已经保证当前follower与leader节点的数据是一致的了
            //如果 leaderCommit > commitIndex，令 commitIndex 等于 leaderCommit 和 新日志条目索引值中较小的一个
            if (request.getLeaderCommit() > raftNode.getCommitIndex()) {
                int commitIndex = (int) Math.min(request.getLeaderCommit(), raftNode.getLogStore().getLastIndex());

                //设置当前节点的commitIndex
                raftNode.setCommitIndex(commitIndex);

                //设置当前节点最终应用到状态机的索引位置
                raftNode.setLastApplied(commitIndex);
            }

            response.setTerm(raftNode.getTerm());
            raftNode.markStatus(NodeStatus.FOLLOWER);
            // TODO, 是否应当在成功回复之后, 才正式提交? 防止 leader "等待回复"过程中 挂掉.
            return response;

        } catch (Exception e) {
            log.error("[{}] Replication error, cause is: {}", raftNode.getSelfPeer(), e);
            return ReplicationResponse.fail("replication error, cause is: " + e.getCause());
        } finally {
            appendLock.compareAndSet(false, true);
        }

    }

    @Override
    public Class<?> supportType() {
        return ReplicationRequest.class;
    }
}
