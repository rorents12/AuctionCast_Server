package main;

import org.json.JSONObject;

public class ServerMessageCompressor {
    public JSONObject getJSONObject(int type, String id, String text) throws Exception{

        JSONObject message = new JSONObject();

        message.put("type", type);
        message.put("id", id);
        message.put("text", text);

        return message;
    }

    public String getJSONObjectToString(int type, String id, String text) throws Exception{
        JSONObject message = new JSONObject();

        message.put("type", type);
        message.put("id", id);
        message.put("text", text);

        return message.toString();
    }

    public String getJSONObjectToString(int type, String id, String text, String roomCode) throws Exception{
        JSONObject message = new JSONObject();

        message.put("type", type);
        message.put("id", id);
        message.put("text", text);
        message.put("roomCode", roomCode);

        return message.toString();
    }

    public String getJSONObjectToString(int type, String id, String text, String roomCode, JSONObject auctionInfo) throws Exception{
        JSONObject message = new JSONObject();

        message.put("type", type);
        message.put("id", id);
        message.put("text", text);
        message.put("roomCode", roomCode);
        message.put("auctionInfo", auctionInfo);

        return message.toString();
    }
}
