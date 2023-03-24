package com.dc.raft.jraft.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class IncrementAndGetRequest implements Serializable {

    private long delta;

}
