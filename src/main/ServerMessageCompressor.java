package main;

import org.json.JSONObject;

/**
 * message compressor
 * 클라이언트로 채팅 메시지를 보낼 때, 양식에 맞게 보내기 위해 JSONObject 형식의 String 으로 여러 요소들을 compress
 * 해주는 method 들을 제공.
 */

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

    public String getJSONObjectToString(int type, String id, String text, String roomCode, String timeStamp) throws Exception{
        JSONObject message = new JSONObject();

        message.put("type", type);
        message.put("id", id);
        message.put("text", text);
        message.put("roomCode", roomCode);
        message.put("timeStamp", timeStamp);

        return message.toString();
    }
}
