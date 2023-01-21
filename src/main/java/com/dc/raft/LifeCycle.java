package com.dc.raft;

/**
 *
 * 生命周期
 * @author zhangyang
 */
public interface LifeCycle {

    void start() throws Exception;

    void stop();

}
