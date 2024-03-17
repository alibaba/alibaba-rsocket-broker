package com.alibaba.rsocket.encoding.impl;

import com.alibaba.rsocket.observability.RsocketErrorCode;
import com.caucho.hessian.io.Deserializer;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.hessian.io.SerializerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Hessian's serializer factory with black class list
 *
 * @author linux_china
 */
public class HessianSerializerFactoryWithBlackList extends SerializerFactory {
    public static final Set<String> BLACK_CLASSES = new HashSet<>();

    static {
        BLACK_CLASSES.add("org.springframework.context.support.ClassPathXmlApplicationContext");
        BLACK_CLASSES.add("javax.swing.UIDefaults$ProxyLazyValue");
    }

    @Override
    public Deserializer getObjectDeserializer(String type, Class cl) throws HessianProtocolException {
        if (BLACK_CLASSES.contains(type)) {
            throw new HessianProtocolException(RsocketErrorCode.message("RST-700401", type));
        }
        return super.getObjectDeserializer(type, cl);
    }
}
