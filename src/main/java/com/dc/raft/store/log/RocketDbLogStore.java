package com.dc.raft.store.log;

import com.dc.raft.LifeCycle;
import com.dc.raft.node.RaftNode;
import com.dc.raft.node.RaftPeers;
import com.dc.raft.store.LogEntry;
import com.google.common.collect.Lists;
import jodd.util.Bits;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Slf4j
public class RocketDbLogStore implements LogStore, LifeCycle {

    private RocksDB rocksDB;

    private static final Path baseDir = Paths.get("./raft");

    private Path logDir;

    private RaftPeers.PeerNode selfNode;

    private ColumnFamilyHandle logStoreHandle;

    private AtomicLong currentIndex;

    private Lock writeLock = new ReentrantLock(false);

    /**
     * 用于记录最后一个logIndex
     */
    private static final String LAST_LOG_INDEX_KEY = "LAST_LOG_INDEX";
    private static final String LOG_STORE_COLUMN_FAMILY_NAME = "LOG_STORE_COLUMN_FAMILY_NAME";

    public RocketDbLogStore(RaftPeers.PeerNode selfNode){
        this.selfNode = selfNode;
    }

    @Override
    public void start() throws Exception {
        int selfPort = selfNode.getAddress().getPort();
        //日志地址：/raft/8080/log
        logDir = baseDir.resolve(String.valueOf(selfPort)).resolve("log");

        System.out.println(logDir.toFile().getAbsolutePath());
        DBOptions options = new DBOptions()
                .setCreateIfMissing(true)
                .setCreateMissingColumnFamilies(true);

        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

        try (ColumnFamilyOptions familyOptions = new ColumnFamilyOptions().optimizeUniversalStyleCompaction()) {
            //默认列组
            ColumnFamilyDescriptor defaultFamily = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, familyOptions);
            //用于存放raft日志的列组
            ColumnFamilyDescriptor logStoreFamily = new ColumnFamilyDescriptor(LOG_STORE_COLUMN_FAMILY_NAME.getBytes(StandardCharsets.UTF_8), familyOptions);
            List<ColumnFamilyDescriptor> descriptors = Lists.newArrayList(defaultFamily, logStoreFamily);
            List<ColumnFamilyHandle> familyHandles = Lists.newArrayList();
            this.rocksDB = RocksDB.open(options, logDir.toString(), descriptors, familyHandles);

            //从开启的familyHandles中找到用于LogStore的familyHandle
            logStoreHandle = familyHandles.stream().filter(handle -> {
                try {
                    return new String(handle.getName()).equals(LOG_STORE_COLUMN_FAMILY_NAME);
                } catch (RocksDBException e) {
                    log.error("Get column family for {} error: ", LOG_STORE_COLUMN_FAMILY_NAME, e);
                }
                return false;
            }).findFirst().orElseGet(() -> rocksDB.getDefaultColumnFamily());
        }

        //由于生成新的日志index
        this.currentIndex = new AtomicLong(this.getLastIndex());
    }

    @Override
    public void stop() {
        logStoreHandle.close();
        rocksDB.close();
    }

    @Override
    public void write(LogEntry logEntry) {
        try {
            //要保证原子性
            writeLock.lock();
            long index = getLastIndex() + 1;
            logEntry.setIndex(index);
            byte[] indexBytes = getLongBytes(index);
            rocksDB.put(logStoreHandle, indexBytes, logEntry.serialize());
            updateLastIndex(index);
        } catch (RocksDBException e) {
            log.error("Put data to rocketDB error: ", e);
            throw new RuntimeException("Put data to rocketDB error");
        } finally {
            writeLock.unlock();
        }

    }


    private void updateLastIndex(Long index) {
        try {
            rocksDB.put(logStoreHandle, LAST_LOG_INDEX_KEY.getBytes(StandardCharsets.UTF_8), getLongBytes(index));
        } catch (RocksDBException e) {
            log.error("Update last index error: ", e);
        }
    }

    @Override
    public LogEntry read(Long logIndex) {
        try {
            byte[] index = getLongBytes(logIndex);
            byte[] bytes = rocksDB.get(logStoreHandle, index);
            if (bytes != null) {
                return LogEntry.deserialize(bytes);
            }
        } catch (RocksDBException e) {
            log.error("Cannot find key from rocketDB, error: ", e);
        }

        return null;
    }

    @Override
    public List<LogEntry> readRange(long startIndex, long endIndex) {
        return LongStream.rangeClosed(startIndex, endIndex)
                .mapToObj(this::read)
                .map(Objects::requireNonNull)
                .collect(Collectors.toList());
    }

    /**
     * 不包括startIndex
     *
     * @param startIndex 从startIndex 之后开始截断
     */
    @Override
    public void truncate(long startIndex) {
        try {
            writeLock.lock();
            //获取最后一条索引
            long truncateLastIndex = getLastIndex() + 1;
            long truncateStartIndex = startIndex + 1;
            rocksDB.deleteRange(logStoreHandle, getLongBytes(truncateStartIndex), getLongBytes(truncateLastIndex));
            updateLastIndex(startIndex);
        } catch (Exception e) {
            log.error("Truncate range from {}-{} error, cause is: ", startIndex, getLastIndex(), e);
        } finally {
            writeLock.unlock();
        }


    }

    @Override
    public Long getLastTerm() {
        LogEntry lastLog = getLastLog();
        return lastLog != null ? lastLog.getTerm() : 0;
    }

    @Override
    public LogEntry getLastLog() {
        try {
            byte[] valueBytes = rocksDB.get(logStoreHandle, getLongBytes(getLastIndex()));
            if (valueBytes != null) {
                return LogEntry.deserialize(valueBytes);
            }

        } catch (Exception e) {
            log.error("Get last log error, error is :", e);
        }

        return null;
    }

    public byte[] getLongBytes(long value) {
        byte[] bytes = new byte[8];
        Bits.putLong(bytes, 0, value);
        return bytes;
    }

    @Override
    public Long getLastIndex() {
        long lastIndex = -1;
        try {
            byte[] bytes = rocksDB.get(logStoreHandle, LAST_LOG_INDEX_KEY.getBytes(StandardCharsets.UTF_8));
            if (bytes != null && bytes.length >= 8) {
                lastIndex = Bits.getLong(bytes, 0);
            }
        } catch (RocksDBException e) {
            log.error("rocket find property error: ", e);
        }
        return lastIndex;
    }

    @Override
    public LogEntry getPrevLog(long logIndex) {

        //如果logIndex == 0 的时候则不在读取数据
        if (logIndex == 0) {
            return null;
        }

        long prevIndex = logIndex - 1;
        LogEntry logEntry = read(prevIndex);
        if (logEntry != null) {
            return logEntry;
        }

        return getPrevLog(prevIndex);
    }

    public static void main(String[] args) throws Exception {
        RocketDbLogStore dbLogStore = new RocketDbLogStore(RaftPeers.PeerNode.create(new InetSocketAddress(8080)));

        dbLogStore.start();

        ColumnFamilyHandle logStoreHandle = dbLogStore.logStoreHandle;

        dbLogStore.rocksDB.put(logStoreHandle, "1".getBytes(StandardCharsets.UTF_8), "2".getBytes(StandardCharsets.UTF_8));
        dbLogStore.rocksDB.put(dbLogStore.rocksDB.getDefaultColumnFamily(), "1".getBytes(StandardCharsets.UTF_8), "4".getBytes(StandardCharsets.UTF_8));

        System.out.println(new String(dbLogStore.rocksDB.get(logStoreHandle, "1".getBytes(StandardCharsets.UTF_8))));
        System.out.println(Arrays.toString(dbLogStore.rocksDB.get(dbLogStore.rocksDB.getDefaultColumnFamily(), "1".getBytes(StandardCharsets.UTF_8))));

        LogEntry testKey = LogEntry.newBuilder().key("testKey").value(null).term(1).index(2L).build();
        dbLogStore.write(testKey);

        LogEntry lastLog = dbLogStore.getLastLog();

        dbLogStore.truncate(1);

        System.out.println(lastLog);
        System.out.println(dbLogStore.getLastIndex());
        System.out.println(dbLogStore.getLastLog());

        dbLogStore.stop();

    }
}
