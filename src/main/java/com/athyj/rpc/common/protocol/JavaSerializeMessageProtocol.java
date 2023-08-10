package com.athyj.rpc.common.protocol;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Java序列化消息协议
 */
public class JavaSerializeMessageProtocol implements MessageProtocol {

    private static final ThreadLocal<Kryo> kryoLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setReferences(true);//检测循环依赖，默认值为true,避免版本变化显式设置
        kryo.setRegistrationRequired(false);//默认值为true，避免版本变化显式设置
        ((Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy())
                .setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());//设定默认的实例化器
        return kryo;
    });

    private Kryo getKryo() {
        return kryoLocal.get();
    }

    private byte[] serialize(Object obj) throws Exception {
        Kryo kryo = getKryo();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        kryo.writeClassAndObject(output, obj);
        output.close();
        return byteArrayOutputStream.toByteArray();
    }

    public <T> T deserialize(byte[] bytes) {
        Kryo kryo = getKryo();
        Input input = new Input(new ByteArrayInputStream(bytes));
        return (T) kryo.readClassAndObject(input);
    }

    /*private byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(obj);
        return bout.toByteArray();
    }*/

    /*public <T> T deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(data));
        return (T) in.readObject();
    }*/

    @Override
    public byte[] marshallingRequest(LeisureRequest req) throws Exception {
        return this.serialize(req);
    }

    @Override
    public LeisureRequest unmarshallingRequest(byte[] data) throws Exception {
        return deserialize(data);
    }

    @Override
    public byte[] marshallingResponse(LeisureResponse rsp) throws Exception {
        return this.serialize(rsp);
    }

    @Override
    public LeisureResponse unmarshallingResponse(byte[] data) throws Exception {
        return deserialize(data);
    }
}
