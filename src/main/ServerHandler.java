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
    private static final ServerMessageParser parser = new ServerMessageParser();
    private static final ServerMessageCompressor compressor = new ServerMessageCompressor();

    private static HashMap<String, JSONObject> hashMap_AuctionInfo = new HashMap();

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

        Channel incoming;
        ChannelGroup room;
        JSONObject json;

        //처음 보내는 메시지로 방을 만든 것인지, 방에 참여한것인지 구분하고 방을 만든 것이라면 channelgroup을 만들고,
        //방에 참여한것이라면 channelgroup에 channel을 추가해준다.

        switch (parser.getMessageType(message)){
            case global_variable.CODE_CHAT_MAKEROOM:
                //방을 만들었을 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());

                room = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

                incoming = ctx.channel();
                room.add(incoming);

                hashMap_ChannelGroup.put(parser.getMessageId(message), room);
                break;

            case global_variable.CODE_CHAT_ENTRANCE:
                //방에 들어왔을 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());

                incoming = ctx.channel();
                room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));

                room.add(incoming);
                break;

            case global_variable.CODE_CHAT_EXIT:
                //방을 나갈 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());
                incoming = ctx.channel();

                if(parser.getMessageId(message).equals(parser.getMessageRoomCode(message))){
                    hashMap_ChannelGroup.remove(parser.getMessageRoomCode(message));
                }else {
                    room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));
                    room.remove(incoming);
                }
                break;

            case global_variable.CODE_CHAT_MESSAGE_GENERAL:
                //채팅 메시지가 왔을 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());
                incoming = ctx.channel();
                room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));

                String roomCode = parser.getMessageRoomCode(message);

                for (Channel channel : room) {
                    if (channel != incoming){

                        channel.writeAndFlush(
                                compressor.getJSONObjectToString(
                                        parser.getMessageType(message),
                                        parser.getMessageId(message),
                                        parser.getMessageText(message)) +"\n"
                        );

                    }
                }
                break;

            case global_variable.CODE_CHAT_START_AUCTION:
                // 경매 시작 요청이 왔을 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());
                incoming = ctx.channel();
                room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));

                json = new JSONObject();
                json.put("price", parser.getMessageText(message));
                json.put("id", "");

                hashMap_AuctionInfo.put(parser.getMessageRoomCode(message), json);

                for (Channel channel : room) {
                    channel.writeAndFlush(
                        compressor.getJSONObjectToString(
                                parser.getMessageType(message),
                                parser.getMessageId(message),
                                parser.getMessageText(message),
                                parser.getMessageRoomCode(message),
                                hashMap_AuctionInfo.get(parser.getMessageRoomCode(message))) + "\n"
                    );
                }
                break;

            case global_variable.CODE_CHAT_STOP_AUCTION:
                // 경매 종료 요청이 왔을 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());
                incoming = ctx.channel();
                room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));

                for (Channel channel : room) {
                    channel.writeAndFlush(
                            compressor.getJSONObjectToString(
                                    parser.getMessageType(message),
                                    parser.getMessageId(message),
                                    parser.getMessageText(message),
                                    parser.getMessageRoomCode(message),
                                    hashMap_AuctionInfo.get(parser.getMessageRoomCode(message))) + "\n"
                    );
                }

                break;

            case global_variable.CODE_CHAT_PRICE_RAISE:
                // 경매 입찰 요청이 들어왔을 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());
                incoming = ctx.channel();
                room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));

                JSONObject j = hashMap_AuctionInfo.get(parser.getMessageRoomCode(message));
                int price_now = Integer.parseInt(j.getString("price"));
                int price_requested = Integer.parseInt(parser.getMessageText(message));

                // 경매 참가자가 올린 입찰가가 현재 최고입찰가보다 낮은경우, break 하여 요청을 무시한다.
                if(price_requested <= price_now){
                    break;
                }

                // 최고 입찰가를 갱신하고 해당 입찰가를 입력한 사용자의 이름을 기록한다.
                json = new JSONObject();
                json.put("price", parser.getMessageText(message));
                json.put("id", parser.getMessageId(message));

                hashMap_AuctionInfo.put(parser.getMessageRoomCode(message), json);

                for (Channel channel : room) {
                    channel.writeAndFlush(
                            compressor.getJSONObjectToString(
                                    parser.getMessageType(message),
                                    parser.getMessageId(message),
                                    parser.getMessageText(message),
                                    parser.getMessageRoomCode(message),
                                    hashMap_AuctionInfo.get(parser.getMessageRoomCode(message))) + "\n"
                    );
                }

                break;
        }

//        if(parser.getMessageType(message) == global_variable.CODE_CHAT_MAKEROOM){
//
//
//        }else if(parser.getMessageType(message) == global_variable.CODE_CHAT_ENTRANCE){
//
//
//        }else if(parser.getMessageType(message) == global_variable.CODE_CHAT_MESSAGE_GENERAL){
//
//        }else if(parser.getMessageType(message) == global_variable.CODE_CHAT_EXIT){
//
//        }

    }

}