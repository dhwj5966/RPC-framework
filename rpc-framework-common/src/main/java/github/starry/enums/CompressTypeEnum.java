package github.starry.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Starry
 * @create 2023-01-27-12:25 PM
 * @Describe 压缩类型枚举类
 */
@AllArgsConstructor
@Getter
public enum CompressTypeEnum {

    GZIP((byte) 0x01, "gzip");

    private final byte code;
    private final String name;

    public static String getName(byte code) {
        for (CompressTypeEnum c : CompressTypeEnum.values()) {
            if (c.getCode() == code) {
                return c.name;
            }
        }
        return null;
    }

}


