package github.starry.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Starry
 * @create 2023-01-27-12:15 PM
 * @Describe 枚举类，枚举所有序列化方式。
 */
@AllArgsConstructor
@Getter
public enum SerializationTypeEnum {

    KRYO((byte) 0x01, "kryo");

    private final byte code;

    private final String name;

    public static String getName(byte code0) {
        for (SerializationTypeEnum s : SerializationTypeEnum.values()) {
            if (s.code == code0) {
                return s.name;
            }
        }
        return null;
    }

}
