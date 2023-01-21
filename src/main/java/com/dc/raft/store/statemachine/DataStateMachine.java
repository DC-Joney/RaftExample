package com.dc.raft.store.statemachine;

import com.dc.raft.LifeCycle;
import com.dc.raft.store.LogEntry;

/**
 * 状态机，用于存储日志信息
 */
public interface DataStateMachine extends LifeCycle {

    /**
     * 将数据应用到状态机.
     *
     * 原则上,只需这一个方法(apply). 其他的方法是为了更方便的使用状态机.
     * @param logEntry 日志中的数据.
     */
    void apply(LogEntry logEntry);

    /**
     * 获取logKey对应的日志
     * @param key key
     */
    LogEntry getLog(String key);


    /**
     * 删除key 对应的数据
     * @param deleteKeys 要被删除的日志key
     */
    boolean delete(String  deleteKey);

}
