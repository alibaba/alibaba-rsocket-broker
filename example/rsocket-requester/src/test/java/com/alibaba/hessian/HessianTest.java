package com.alibaba.hessian;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * hessian test
 *
 * @author leijuan
 */
public class HessianTest {
    Object[] args = {"demo", 1, null, "demo"};
    byte[] content;

    @Test
    public void testEncode() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HessianSerializerOutput output = new HessianSerializerOutput(bos);
        output.writeObject(args);
        output.flush();
        content = bos.toByteArray();
        System.out.println(content.length);
    }

    @Test
    public void testEncodeInteger() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HessianSerializerOutput output = new HessianSerializerOutput(bos);
        output.writeObject(1);
        output.flush();
        content = bos.toByteArray();
        Assertions.assertEquals(content[0], -111);
    }

    @Test
    public void testDecode() throws Exception {
        testEncode();
        ByteArrayInputStream bis = new ByteArrayInputStream(content);
        HessianSerializerInput input = new HessianSerializerInput(bis);
        Object[] object = (Object[]) input.readObject();
        System.out.println(object);
    }
}
