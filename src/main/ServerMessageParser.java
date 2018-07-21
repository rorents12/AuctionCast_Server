package main;

import org.json.JSONObject;

/**
 * message parser
 * 클라이언트로부터 받은 메시지를 처리하고자 할 때, JSONObject 형식의 String 으로 되어있는 message 에서 원하는 정보들을
 * 빼서 String 으로 반환해주는 method 들을 제공.
 */

public class ServerMessageParser {

    private JSONObject message;

    public int getMessageType(String msg){
        this.message = new JSONObject(msg);

        return message.getInt("type");
    }

    public String getMessageId(String msg) throws Exception {
        this.message = new JSONObject(msg);

        return message.getString("id");
    }

    public String getMessageText(String msg){
        this.message = new JSONObject(msg);

        return message.getString("text");
    }

    public String getMessageRoomCode(String msg){
        this.message = new JSONObject(msg);

        return message.getString("roomCode");
    }

    public JSONObject getMessageAuctionInfo(String msg){
        this.message = new JSONObject(msg);

        return message.getJSONObject("auctionInfo");
    }

    public String getMessageTimeStamp(String msg){
        this.message = new JSONObject(msg);

        return message.getString("timeStamp");
    }

}
