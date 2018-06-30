package main;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.json.JSONObject;

import java.util.HashMap;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    private static HashMap hashMap_ChannelGroup = new HashMap();
//    private static final ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final ServerMessageParser parser = new ServerMessageParser();
    private static final ServerMessageCompressor compressor = new ServerMessageCompressor();

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerAdded of [SERVER]");
//        Channel incoming = ctx.channel();

//        for(Channel channel : channelGroup){
//            //사용자가 추가되었을 때 기존 사용자에게 알림
//            channel.write("[SERVER] - " + incoming.remoteAddress() + "has joined!\n");
//        }

//        channelGroup.add(incoming);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 사용자가 접속했을 때 서버에 표시
        System.out.println("User Access!");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerRemoved of [SERVER]");
        Channel incoming = ctx.channel();
//        for(Channel channel : channelGroup) {
//            //사용자가 나갔을 때 기존 사용자에게 알림
//            channel.write("[SERVER] - " + incoming.remoteAddress() + "has left!\n");
//        }
//        channelGroup.remove(incoming);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = null;
        message = (String)msg;

        //처음 보내는 메시지로 방을 만든 것인지, 방에 참여한것인지 구분하고 방을 만든 것이라면 channelgroup을 만들고,
        //방에 참여한것이라면 channelgroup에 channel을 추가해준다.
        if(parser.getMessageType(message) == global_variable.CODE_CHAT_MAKEROOM){
            //방을 만들었을 때
            System.out.println("channelRead of [SERVER]" + message.toLowerCase());

            ChannelGroup room = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

            Channel incoming = ctx.channel();
            room.add(incoming);

            hashMap_ChannelGroup.put(parser.getMessageId(message), room);

        }else if(parser.getMessageType(message) == global_variable.CODE_CHAT_ENTRANCE){
            //방에 들어왔을 때
            System.out.println("channelRead of [SERVER]" + message.toLowerCase());

            Channel incoming = ctx.channel();
            ChannelGroup room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));

            room.add(incoming);

        }else if(parser.getMessageType(message) == global_variable.CODE_CHAT_MESSAGE_GENERAL){
            System.out.println("channelRead of [SERVER]" + message.toLowerCase());
            Channel incoming = ctx.channel();
            ChannelGroup room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));

            for (Channel channel : room) {
                if (channel != incoming){
                    channel.writeAndFlush(compressor.getJSONObjectToString(parser.getMessageType(message), parser.getMessageId(message), parser.getMessageText(message)) + "\n");
                 }
            }
        }else if(parser.getMessageType(message) == global_variable.CODE_CHAT_EXIT){
            System.out.println("channelRead of [SERVER]" + message.toLowerCase());
            Channel incoming = ctx.channel();

            if(parser.getMessageId(message).equals(parser.getMessageRoomCode(message))){
                hashMap_ChannelGroup.remove(parser.getMessageRoomCode(message));
            }else {
                ChannelGroup room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));
                room.remove(incoming);
            }
        }

    }
}