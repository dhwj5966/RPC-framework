package github.starry.exception;

import github.starry.enums.RpcErrorMessageEnum;

/**
 * @author Starry
 * @create 2023-01-26-1:33 PM
 * @Describe
 */
public class RpcException extends RuntimeException{
    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum, String detail) {
        super(rpcErrorMessageEnum.getMessage() + ":" + detail);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(RpcErrorMessageEnum rpcErrorMessageEnum) {
        super(rpcErrorMessageEnum.getMessage());
    }
}
