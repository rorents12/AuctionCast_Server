package main;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.HashMap;

public class ServerHandler extends ChannelInboundHandlerAdapter {

    // 채팅방 정보를 담고 있는 HashMap
    private static HashMap hashMap_ChannelGroup = new HashMap();

    // 채팅방에서 진행중인 경매의 진행상황을 채팅방 별로 관리할 수 있는 HashMap
    private static HashMap<String, JSONObject> hashMap_AuctionInfo = new HashMap();

    // 클라이언트로 부터 오는 채팅 메시지를 parsing 하고,
    // 클라이언트로 보낼 메시지를 compress 해주는 utility class
    private static final ServerMessageParser parser = new ServerMessageParser();
    private static final ServerMessageCompressor compressor = new ServerMessageCompressor();


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerAdded of [SERVER]");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 사용자가 접속했을 때 서버에 표시
        System.out.println("User Access!");
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        System.out.println("handlerRemoved of [SERVER]");
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

        // 클라이언트로부터 받은 메시지를 parsing 하여 messageType 으로부터 Chatting Code 를 파악하고,
        // 해당 code 에 따라 처리를 진행한다.
        switch (parser.getMessageType(message)){
            case global_variable.CODE_CHAT_MAKEROOM:
                // 방을 만들었을 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());

                // 새로운 ChannelGroup 을 만들고, 해당 그룹에 방송자의 channel 을 삽입한다.
                room = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
                incoming = ctx.channel();
                room.add(incoming);

                // 새로운 채널 그룹을 HashMap 에 저장한다. key 값은 방송자가 속한 방송의 roomCode 로 지정한다.
                hashMap_ChannelGroup.put(parser.getMessageRoomCode(message), room);
                break;

            case global_variable.CODE_CHAT_ENTRANCE:
                //방에 들어왔을 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());

                // 시청자가 방송에 들어왔을 때, ChannelGroup HashMap 에서 현재 시청자가 속한 방송의 roomCode 를 이용해
                // ChannelGroup 을 불러온 후, 시청자의 channel 을 해당 그룹에 삽입한다.
                incoming = ctx.channel();
                room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));

                room.add(incoming);
                break;

            case global_variable.CODE_CHAT_EXIT:
                //방을 나갈 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());
                incoming = ctx.channel();

                // 방을 나가는 사람이 방송자라면 해당 방송을 종료하고 채팅방 ChannelGroup 을 없앤다.
                // 방을 나가는 사람이 시청자라면 해당 방송의 ChannelGroup 에서 시청자의 channel 만 삭제한다.
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
                String roomCode = parser.getMessageRoomCode(message);
                room = (ChannelGroup) hashMap_ChannelGroup.get(roomCode);

                // 채팅 메시지를 jedis 를 통해 redis 에 저장한다.
                // 저장시 hash 형태로 저장하며 key 값은 roomCode, field 값은 timeStamp, value 값은 JSON 형식의 채팅 메시지이다.
                JedisPool jedisPool = new JedisPool(new JedisPoolConfig(), "127.0.0.1");
                Jedis jedis = jedisPool.getResource();
                try{
                    jedis.hset(
                            roomCode,
                            parser.getMessageTimeStamp(message),
                            compressor.getJSONObjectToString(
                                    parser.getMessageType(message),
                                    parser.getMessageId(message),
                                    parser.getMessageText(message))
                    );
                }catch(JedisConnectionException e){
                    if(null != jedis){
                        jedisPool.returnBrokenResource(jedis);
                        jedis = null;
                    }
                }finally{
                    if(null != jedis){
                        jedisPool.returnResource(jedis);
                    }
                }
                jedisPool.destroy();

                // 채팅을 보낸 당사자를 제외한 모든 채팅방의 사용자에게 채팅 메시지를 보낸다.
                for (Channel channel : room) {
                    if (channel != incoming){
                        String s = compressor.getJSONObjectToString(
                                parser.getMessageType(message),
                                parser.getMessageId(message),
                                parser.getMessageText(message),
                                parser.getMessageRoomCode(message),
                                parser.getMessageTimeStamp(message)) +"\n";
                        channel.writeAndFlush(s);

                    }
                }
                break;

            case global_variable.CODE_CHAT_START_AUCTION:
                // 경매 시작 요청이 왔을 때
                System.out.println("channelRead of [SERVER]" + message.toLowerCase());
                incoming = ctx.channel();
                room = (ChannelGroup) hashMap_ChannelGroup.get(parser.getMessageRoomCode(message));

                // jsonObject 를 하나 만들고, dummy 값을 삽입한 후 HashMap_AuctionInfo 에 roomCode 를 key 값으로 저장한다.
                // 이 jsonObject 는 경매 입찰이 갱신될 때마다 정보가 갱신되며, 경매 종료 시 해당 jsonObject 를 사용자들에게
                // 전송하여 어떤 사용자가 얼마의 낙찰가에 낙찰을 받았는지 알릴 수 있다.
                json = new JSONObject();
                json.put("price", parser.getMessageText(message));
                json.put("id", "");
                hashMap_AuctionInfo.put(parser.getMessageRoomCode(message), json);

                // 채팅방의 모든 사용자들에게 경매 시작 신호를 전송한다.
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

                // 채팅방의 모든 사용자들에게 경매 종료 신호를 전송한다.
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

                // 채팅방의 모든 사용자에게 입찰 갱신 정보를 전송한다.
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

    }

}