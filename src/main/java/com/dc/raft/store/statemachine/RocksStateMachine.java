package com.dc.raft.store.statemachine;

import com.dc.raft.LifeCycle;
import com.dc.raft.node.RaftPeers;
import com.dc.raft.store.LogEntry;
import com.google.common.collect.Lists;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.*;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class RocksStateMachine implements DataStateMachine, LifeCycle {

    private RocksDB rocksDB;

    private static final Path baseDir = Paths.get("./raft");

    private Path dataDir;

    @Setter
    private RaftPeers raftPeers;

    private ColumnFamilyHandle logStoreHandle;

    private static final String DATA_STORE_COLUMN_FAMILY_NAME = "DATA_STORE_COLUMN_FAMILY_NAME";


    public RocksStateMachine(RaftPeers.PeerNode selfPeer) {
        int selfPort = selfPeer.getAddress().getPort();
        //日志地址：/raft/8080/log
        dataDir = baseDir.resolve(String.valueOf(selfPort)).resolve("store");
    }

    @Override
    public void start() throws Exception {
        initRocksDB();
    }

    private void initRocksDB() throws Exception {

        DBOptions options = new DBOptions()
                .setCreateIfMissing(true)
                .setCreateMissingColumnFamilies(true);

        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }

        try (ColumnFamilyOptions familyOptions = new ColumnFamilyOptions().optimizeUniversalStyleCompaction()) {
            //默认列组
            ColumnFamilyDescriptor defaultFamily = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, familyOptions);
            //用于存放raft日志的列组
            ColumnFamilyDescriptor logStoreFamily = new ColumnFamilyDescriptor(DATA_STORE_COLUMN_FAMILY_NAME.getBytes(StandardCharsets.UTF_8), familyOptions);
            List<ColumnFamilyDescriptor> descriptors = Lists.newArrayList(defaultFamily, logStoreFamily);
            List<ColumnFamilyHandle> familyHandles = Lists.newArrayList();
            this.rocksDB = RocksDB.open(options, dataDir.toString(), descriptors, familyHandles);

            //从开启的familyHandles中找到用于LogStore的familyHandle
            logStoreHandle = familyHandles.stream().filter(handle -> {
                try {
                    return new String(handle.getName()).equals(DATA_STORE_COLUMN_FAMILY_NAME);
                } catch (RocksDBException e) {
                    log.error("Get column family for {} error: ", DATA_STORE_COLUMN_FAMILY_NAME, e);
                }
                return false;
            }).findFirst().orElseGet(() -> rocksDB.getDefaultColumnFamily());
        }

    }

    @Override
    public void stop() {
        logStoreHandle.close();
        rocksDB.close();
    }

    @Override
    public void apply(LogEntry logEntry) {
        try {
            String key = logEntry.getKey();
            if (StringUtils.isEmpty(key)) {
                throw new RuntimeException("log key must not be null");
            }
            rocksDB.put(logStoreHandle, key.getBytes(StandardCharsets.UTF_8), logEntry.serialize());
        } catch (RocksDBException e) {
            log.error("Store log command error: ", e);
        }
    }

    @Override
    public LogEntry getLog(String key) {
        if (StringUtils.isEmpty(key)) {
            throw new RuntimeException("log key must not be null");
        }

        try {
            byte[] bytes = rocksDB.get(logStoreHandle, key.getBytes(StandardCharsets.UTF_8));
            if (bytes != null && bytes.length > 0) {
                return LogEntry.deserialize(bytes);
            }
        } catch (RocksDBException e) {
            log.error("Get log command error: ", e);
        }
        return null;
    }


    @Override
    public boolean delete(String deleteKey) {
        try {
            if (StringUtils.hasText(deleteKey)) {
                rocksDB.delete(logStoreHandle, deleteKey.getBytes(StandardCharsets.UTF_8));
                return true;
            }
        } catch (Exception e) {
            log.error("Delete log key error: ", e);
        }

        return false;
    }
}
