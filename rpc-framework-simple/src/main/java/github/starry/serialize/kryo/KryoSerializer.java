package github.starry.serialize.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import github.starry.remoting.message.RpcRequest;
import github.starry.remoting.message.RpcResponse;
import github.starry.serialize.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.function.Supplier;

/**
 * @author Starry
 * @create 2023-01-27-12:46 AM
 * @Describe Kryo序列化实现类。
 */
public class KryoSerializer implements Serializer {
    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(new Supplier<Kryo>() {
        @Override
        public Kryo get() {
            Kryo kryo = new Kryo();
            kryo.register(RpcRequest.class);
            kryo.register(RpcResponse.class);
            return kryo;
        }
    });

    @Override
    public byte[] serialize(Object object) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);

        Kryo kryo = kryoThreadLocal.get();
        kryo.writeObject(output, object);
        kryoThreadLocal.remove();
        return output.toBytes();
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] data) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
        Input input = new Input(byteArrayInputStream);

        Kryo kryo = kryoThreadLocal.get();
        // byte->Object:从byte数组中反序列化出对对象
        Object o = kryo.readObject(input, clazz);
        kryoThreadLocal.remove();
        return clazz.cast(o);
    }

}
