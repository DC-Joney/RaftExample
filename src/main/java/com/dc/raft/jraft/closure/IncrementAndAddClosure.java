package com.dc.raft.jraft.closure;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Status;
import com.dc.raft.jraft.CounterServer;
import com.dc.raft.jraft.dto.IncrementAndGetRequest;
import com.dc.raft.jraft.dto.ValueResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class IncrementAndAddClosure implements Closure {

    private CounterServer counterServer;

    @Getter
    @Setter
    private IncrementAndGetRequest request;

    @Getter
    private ValueResponse response;

    @NonNull
    private Closure done;

    @Override
    public void run(Status status) {
        if (done != null)
            done.run(status);

    }

}
