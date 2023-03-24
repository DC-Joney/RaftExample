package com.dc.raft.jraft.statemachine;

import com.alipay.remoting.codec.Codec;
import com.alipay.remoting.exception.CodecException;
import com.alipay.remoting.serialization.SerializerManager;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.core.StateMachineAdapter;
import com.caucho.hessian.io.Hessian2Input;
import com.dc.raft.jraft.closure.IncrementAndAddClosure;
import com.dc.raft.jraft.dto.IncrementAndGetRequest;
import com.sun.xml.internal.ws.api.pipe.Codecs;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class CounterStateMachine extends StateMachineAdapter {


    /**
     * counter value
     */
    private AtomicLong value = new AtomicLong(0);


    @Override
    public void onApply(Iterator iter) {


        while (iter.hasNext()) {
            long delta = 0;
            if (iter.done() != null) {
                IncrementAndAddClosure addClosure = (IncrementAndAddClosure) iter.done();
                delta = addClosure.getRequest().getDelta();
            }else {
                ByteBuffer data = iter.getData();
                try {


                    SerializerManager.getSerializer(SerializerManager.Hessian2);

                    IncrementAndGetRequest request = Codecs.getSerializer(Codecs.Hessian2).decode(data.array(),
                            IncrementAndGetRequest.class.getName());
                    delta = request.getDelta();
                } catch (CodecException e) {
                    log.error("Fail to decode IncrementAndGetRequest", e);
                }

            }



        }

    }

    @Override
    public void onLeaderStart(long term) {
        super.onLeaderStart(term);
    }

    @Override
    public void onLeaderStop(Status status) {
        super.onLeaderStop(status);
    }


}
