package com.dc.raft.log;

import com.dc.raft.LifeCycle;
import com.dc.raft.node.RaftPeers;
import com.google.common.collect.Lists;
import jodd.util.Bits;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.C;
import org.rocksdb.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class RocketDbLogStore implements LogStore, LifeCycle {

    private RocksDB rocksDB;

    private static final Path baseDir = Paths.get("./raft");

    private Path logDir;

    @Setter
    private RaftPeers raftPeers;

    private ColumnFamilyHandle logStoreHandle;

    private AtomicLong currentIndex;

    /**
     * 用于记录最后一个logIndex
     */
    private static final String LAST_LOG_INDEX_KEY = "LAST_LOG_INDEX";
    private static final String LOG_STORE_COLUMN_FAMILY_NAME = "LOG_STORE_COLUMN_FAMILY_NAME";

    @Override
    public void start() throws Exception {
        int selfPort = raftPeers.getSelf().getAddress().getPort();
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
            long index = incrementIndex();
            logEntry.setIndex(index);
            byte[] indexBytes = getLongBytes(index);
            rocksDB.put(indexBytes, logEntry.serialize());
            updateLastIndex(index);
        } catch (RocksDBException e) {
            log.error("Put data to rocketDB error: ", e);
            throw new RuntimeException("Put data to rocketDB error");
        }

    }

    private long incrementIndex() {
        return getLastIndex() + 1;
    }

    private void updateLastIndex(Long index) {
        try {
            rocksDB.put(LAST_LOG_INDEX_KEY.getBytes(StandardCharsets.UTF_8), getLongBytes(index));
        } catch (RocksDBException e) {
            log.error("Update last index error: ", e);
        }
    }

    @Override
    public LogEntry read(Long logIndex) {
        try {
            byte[] index = getLongBytes(logIndex);
            byte[] bytes = rocksDB.get(index);
            if (bytes != null) {
                return LogEntry.deserialize(bytes);
            }
        } catch (RocksDBException e) {
            log.error("Cannot find key from rocketDB, error: ", e);
        }

        return null;
    }

    @Override
    public void truncate(Long startIndex) {
        try {
            long lastIndex = getLastIndex();
            rocksDB.deleteRange(getLongBytes(startIndex), getLongBytes(lastIndex));
        } catch (Exception e) {
            log.error("Truncate range from {}-{} error, cause is: ", startIndex, getLastIndex(), e);
        }


    }

    @Override
    public LogEntry getLastLog() {
        try {
            ColumnFamilyHandle columnFamily = rocksDB.createColumnFamily(new ColumnFamilyDescriptor("123".getBytes(StandardCharsets.UTF_8)));
            byte[] valueBytes = rocksDB.get(columnFamily, getLongBytes(getLastIndex()));
            if (valueBytes != null) {
                return LogEntry.deserialize(valueBytes);
            }

        } catch (Exception e) {
            e.printStackTrace();
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
            byte[] bytes = rocksDB.get(LAST_LOG_INDEX_KEY.getBytes(StandardCharsets.UTF_8));
            if (bytes != null && bytes.length >= 8) {
                lastIndex = Bits.getLong(bytes, 0);
            }
        } catch (RocksDBException e) {
            log.error("rocket find property error: ", e);
        }
        return lastIndex;
    }

    public static void main(String[] args) throws Exception {
        RocketDbLogStore dbLogStore = new RocketDbLogStore();
        RaftPeers raftPeers = new RaftPeers(new InetSocketAddress(8080));
        dbLogStore.setRaftPeers(raftPeers);

        dbLogStore.start();

        ColumnFamilyHandle logStoreHandle = dbLogStore.logStoreHandle;

        dbLogStore.rocksDB.put(logStoreHandle, "1".getBytes(StandardCharsets.UTF_8), "2".getBytes(StandardCharsets.UTF_8));
        dbLogStore.rocksDB.put(dbLogStore.rocksDB.getDefaultColumnFamily(), "1".getBytes(StandardCharsets.UTF_8), "4".getBytes(StandardCharsets.UTF_8));

        System.out.println(new String(dbLogStore.rocksDB.get(logStoreHandle, "1".getBytes(StandardCharsets.UTF_8))));
        System.out.println(Arrays.toString(dbLogStore.rocksDB.get(dbLogStore.rocksDB.getDefaultColumnFamily(), "1".getBytes(StandardCharsets.UTF_8))));

        LogEntry testKey = LogEntry.newBuilder().key("testKey").value(null).term(1).index(2L).build();
        dbLogStore.write(testKey);

        LogEntry lastLog = dbLogStore.getLastLog();


        dbLogStore.stop();

    }
}
