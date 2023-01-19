package com.dc.raft.log;

/**
 * 用于添加日志Entry
 */
public interface LogStore {

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
     *
     * 从startIndex - lastIndex的数据删除
     *
     * @param startIndex 开始截断的index
     */
    void truncate(Long startIndex);


    /**
     * 获取最后一条日志
     */
    LogEntry getLastLog();


    /**
     * 获取日志的最后一条索引Index
     */
    Long getLastIndex();
}
