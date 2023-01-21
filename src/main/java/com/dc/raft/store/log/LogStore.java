package com.dc.raft.store.log;

import com.dc.raft.LifeCycle;
import com.dc.raft.store.LogEntry;

import java.util.List;

/**
 * 用于添加日志Entry
 */
public interface LogStore extends LifeCycle {

    /**
     * 写入日志
     * @param logEntry 日志信息
     */
    void write(LogEntry logEntry);


    /**
     * 读取日志
     * @param logIndex 日志索引
     */
    LogEntry read(Long logIndex);


    /**
     * 读取日志
     * @param logIndex 日志索引
     */
    List<LogEntry> readRange(long startIndex, long endIndex);

    /**
     *
     * 从startIndex - lastIndex的数据删除， 不包括startIndex
     *
     * @param startIndex 开始截断的index
     */
    void truncate(long startIndex);


    /**
     * 获取最后一条日志
     */
    LogEntry getLastLog();

    /**
     * 获取最后一条日志的term
     */
    Long getLastTerm();

    /**
     * 获取日志的最后一条索引Index
     */
    Long getLastIndex();

    /**
     * 获取前一条日志entry
     * @param logEntry 日志
     */
    LogEntry getPrevLog(long logIndex);
}
