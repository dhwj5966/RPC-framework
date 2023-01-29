package github.starry.remoting.constants;

/**
 * @author Starry
 * @create 2023-01-27-12:33 AM
 * @Describe
 */
public class RpcConstants {
    public static final byte[] MAGICNUMBER = new byte[]{'s','r','p','c'};

    public static final byte VERSION = 1;

    public static final int HEAD_LENGTH = 16;

    public static final byte REQUEST_TYPE = 1;
    public static final byte RESPONSE_TYPE = 2;

    public static final byte HEARTBEAT_REQUEST_TYPE = 3;
    //pong
    public static final byte HEARTBEAT_RESPONSE_TYPE = 4;
    public static final String PING = "ping";
    public static final String PONG = "pong";

}
