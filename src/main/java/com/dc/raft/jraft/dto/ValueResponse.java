package com.dc.raft.jraft.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class ValueResponse implements Serializable {


    private long value;

    /**
     * 失败情况下的错误信息
     */
    private String errorMsg;


    private boolean success;

    /**
     * 发生了重新选举，需要跳转的新的leader节点
     */
    private String redirect;

}
