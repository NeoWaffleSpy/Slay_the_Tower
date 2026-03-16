package com.Team_Berry.Utils.Files;

import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.Vector2f;
import com.hypixel.hytale.protocol.Vector3f;

import java.util.Map;

public class JSONParser {

    public static Vector2f getVector2(Object data) {
        Map<String, Object> myData = (Map<String, Object>) data;
        return new Vector2f(getFloat(myData.get("x")), getFloat(myData.get("y")));
    }

    public static Vector3f getVector(Object data) {
        Map<String, Object> myData = (Map<String, Object>) data;
        return new Vector3f(getFloat(myData.get("x")), getFloat(myData.get("y")), getFloat(myData.get("z")));
    }

    public static Position getPosition(Object data) {
        Map<String, Object> myData = (Map<String, Object>) data;
        return new Position(getFloat(myData.get("x")), getFloat(myData.get("y")), getFloat(myData.get("z")));
    }

    public static Direction getDirection(Object data) {
        Map<String, Object> myData = (Map<String, Object>) data;
        return new Direction(getFloat(myData.get("yaw")), getFloat(myData.get("pitch")), getFloat(myData.get("roll")));
    }

    public static Float getFloat(Object n) { return n != null ? ((Number) n).floatValue() : null; }
    public static Integer getInt(Object n) { return n != null ? ((Number) n).intValue() : null; }
    public static Long getLong(Object n) { return n != null ? ((Number) n).longValue() : null; }
}
