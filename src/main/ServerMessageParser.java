package main;

import org.json.JSONObject;

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


}
