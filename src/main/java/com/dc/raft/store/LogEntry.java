package com.dc.raft.store;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

@ToString
@Setter
@Getter
@NoArgsConstructor
public class LogEntry implements Serializable {

    /**
     * 日志索引，是连续的
     */
    private Long index;

    /**
     * 添加日志时对应的任期
     */
    private long term;

    /**
     * 日志对应的key
     */
    private String key;

    /**
     * 日志对应的value
     */
    private String value;

    private LogEntry(Builder builder) {
        setIndex(builder.index);
        setTerm(builder.term);
        setKey(builder.key);
        setValue(builder.value);
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    /**
     * 将数据序列化为字节
     */
    public byte[] serialize() {
        ByteBuf writeBuffer = Unpooled.buffer();
        //1、 logIndex
        writeBuffer.writeLong(index);
        //2、term
        writeBuffer.writeLong(term);
        int keySize = StringUtils.hasText(key) ? key.length() : 0;
        //3、key size
        writeBuffer.writeInt(keySize);
        if (keySize > 0) {
            //4、key data
            writeBuffer.writeBytes(key.getBytes(StandardCharsets.UTF_8));
        }

        //5、value size
        int valueSize = StringUtils.hasText(value) ? value.length() : 0;
        if (valueSize > 0) {
            //6、 value-data
            writeBuffer.writeBytes(value.getBytes(StandardCharsets.UTF_8));
        }
        return writeBuffer.array();
    }

    /**
     * 将数据反序列化为对象
     */
    public static LogEntry deserialize(byte[] serializeBytes) {
        Builder builder = newBuilder();
        ByteBuf byteBuf = Unpooled.copiedBuffer(serializeBytes);
        int length = byteBuf.readableBytes();

        long logIndex = byteBuf.readLong();
        long term = byteBuf.readLong();
        builder.index(logIndex).term(term);

        int keySize = byteBuf.readInt();

        if (keySize > 0) {
            byte[] keyBytes = new byte[keySize];
            byteBuf.readBytes(keyBytes);
            String logKey = new String(keyBytes);
            builder.key(logKey);
        }

        int valueSize = byteBuf.readInt();
        if (valueSize > 0) {
            byte[] valueBytes = new byte[valueSize];
            byteBuf.readBytes(valueBytes);
            String logValue = new String(valueBytes);
            builder.value(logValue);
        }

        return builder.build();
    }


    public static final class Builder {

        private Long index;
        private long term;
        private String key;
        private String value;

        private Builder() {
        }

        public Builder index(Long val) {
            index = val;
            return this;
        }

        public Builder term(long val) {
            term = val;
            return this;
        }

        public Builder key(String key) {
            this.key = key;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public LogEntry build() {
            return new LogEntry(this);
        }
    }
}
