package github.starry.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * @author Starry
 * @create 2023-01-23-1:40 PM
 * @Describe
 */
@AllArgsConstructor
@Getter
@ToString
public enum RpcResponseCodeEnum {
    SUCCESS(200, "rpc is successful"),
    FAIL(500, "rpc is fail");

    private final int code;
    private final String message;

}
