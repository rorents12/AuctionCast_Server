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
}
