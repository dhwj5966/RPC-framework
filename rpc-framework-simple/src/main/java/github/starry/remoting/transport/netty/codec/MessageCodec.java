package github.starry.remoting.transport.netty.codec;

import github.starry.compress.Compress;
import github.starry.enums.CompressTypeEnum;
import github.starry.enums.SerializationTypeEnum;
import github.starry.extension.ExtensionLoader;
import github.starry.remoting.constants.RpcConstants;
import github.starry.remoting.message.RpcMessage;
import github.starry.remoting.message.RpcRequest;
import github.starry.remoting.message.RpcResponse;
import github.starry.serialize.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Starry
 * @create 2023-01-27-12:28 AM
 * @Describe 协议格式：魔数-4B，version-1B，整个数据帧的长度-4B，消息类型-1B，压缩类型-1B，序列化类型-1B，requestId-4B，body。
 */
@Slf4j
public class MessageCodec extends ByteToMessageCodec<RpcMessage> {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    //编码逻辑
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf out) throws Exception {
        try {
            out.writeBytes(RpcConstants.MAGICNUMBER);
            out.writeByte(RpcConstants.VERSION);
            // leave a place to write the value of full length
            out.writerIndex(out.writerIndex() + 4);
            byte messageType = rpcMessage.getMessageType();
            out.writeByte(messageType);
            out.writeByte(rpcMessage.getCodec());
            out.writeByte(CompressTypeEnum.GZIP.getCode());


            out.writeInt(ATOMIC_INTEGER.getAndIncrement());
            // build full length
            byte[] bodyBytes = null;
            int fullLength = RpcConstants.HEAD_LENGTH;
            // if messageType is not heartbeat message,fullLength = head length + body length
            if (messageType != RpcConstants.HEARTBEAT_REQUEST_TYPE
                    && messageType != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                // serialize the object
                String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
                log.info("codec name: [{}] ", codecName);
                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
                        .getExtension(codecName);
                bodyBytes = serializer.serialize(rpcMessage.getData());
                // compress the bytes
                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
                        .getExtension(compressName);
                bodyBytes = compress.compress(bodyBytes);
                fullLength += bodyBytes.length;
            }

            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }
            int writeIndex = out.writerIndex();
            out.writerIndex(writeIndex - fullLength + RpcConstants.MAGICNUMBER.length + 1);
            out.writeInt(fullLength);
            out.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("Encode request error!", e);
        }
    }

    //解码逻辑
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //检查魔数
        if (in.readableBytes() >= RpcConstants.HEAD_LENGTH) {
            try {
                Object result = decodeFrame(in);
                out.add(result);
            } catch (Exception e) {
                log.error("Decode frame error!", e);
                throw e;
            }
        } else {
            log.error("too short frame");
        }
    }

    //协议格式：魔数-4B，version-1B，整个数据帧的长度-4B，消息类型-1B，压缩类型-1B，序列化类型-1B，requestId-4B，body。
    //解码，从in中读出数据，
    private Object decodeFrame(ByteBuf in) {
        //检验魔数
        byte[] magic = new byte[RpcConstants.MAGICNUMBER.length];
        in.readBytes(magic);
        for (int i = 0; i < magic.length; i++) {
            if (magic[i] != RpcConstants.MAGICNUMBER[i]) {
                throw new RuntimeException("Unknown magic code");
            }
        }
        //检验Version
        byte version = in.readByte();
        if (version != RpcConstants.VERSION) {
            throw new RuntimeException("version isn't compatible");
        }
        //整个数据帧的长度
        int fullLength = in.readInt();
        //
        byte messageType = in.readByte();
        byte compressType = in.readByte();
        byte codec = in.readByte();
        int requestId = in.readInt();

        RpcMessage rpcMessage = RpcMessage.builder()
                .messageType(messageType)
                .compress(compressType)
                .codec(codec)
                .requestId(requestId).build();

        //如果是ping pong数据，则直接返回。
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            rpcMessage.setData(RpcConstants.PING);
            return rpcMessage;
        }
        if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            rpcMessage.setData(RpcConstants.PONG);
            return rpcMessage;
        }

        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        byte[] bodyData = new byte[bodyLength];
        in.readBytes(bodyData);
        Serializer serializer =
                ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(SerializationTypeEnum.getName(codec));
        Compress compress =
                ExtensionLoader.getExtensionLoader(Compress.class).getExtension(CompressTypeEnum.getName(compressType));
        bodyData = compress.decompress(bodyData);

        if (messageType == RpcConstants.REQUEST_TYPE) {
            RpcRequest rpcRequest = serializer.deserialize(RpcRequest.class, bodyData);
            rpcMessage.setData(rpcRequest);
        }
        if (messageType == RpcConstants.RESPONSE_TYPE) {
            RpcResponse rpcResponse = serializer.deserialize(RpcResponse.class, bodyData);
            rpcMessage.setData(rpcResponse);
        }

        return rpcMessage;
    }

}
