package com.alibaba.rsocket.encoding.hessian.io.java8;

import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.AbstractSerializer;

import java.io.IOException;
import java.time.OffsetTime;


public class OffsetTimeSerializer extends AbstractSerializer {
    public void writeObject(Object obj, AbstractHessianOutput out) throws IOException {
        if (obj == null) {
            out.writeNull();
        } else {
            out.writeObject(new OffsetTimeHandle((OffsetTime) obj));
        }
    }
}
