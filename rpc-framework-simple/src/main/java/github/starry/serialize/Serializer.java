package github.starry.serialize;

import github.starry.extension.SPI;

/**
 * @author Starry
 * @create 2023-01-27-12:42 AM
 * @Describe 序列化接口
 */
@SPI
public interface Serializer {
    /**
     * 序列化
     * @param object
     * @return
     */
    byte[] serialize(Object object);

    /**
     * 反序列化
     * @param clazz 目标类的类型
     * @param data 字节数组
     * @return 目标对象
     */
    <T> T deserialize(Class<T> clazz, byte[] data);
}
