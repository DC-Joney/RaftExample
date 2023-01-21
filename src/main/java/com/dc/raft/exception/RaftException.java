package com.dc.raft.exception;

import cn.hutool.core.util.StrUtil;

/**
 * Raft异常
 */
public class RaftException extends RuntimeException{

    public RaftException(String message, Object...args) {
        super(StrUtil.format(message, args));
    }

    public RaftException( Throwable cause, String message, Object...args) {
        super(StrUtil.format(message, args), cause);
    }

    public RaftException(Throwable cause) {
        super(cause);
    }
}
