package UDP;

import java.util.HashMap;
import java.util.Map;

public enum PacketType {
    DATA(0),
    ACK(1),
    SYN(2),
    SYN_ACK(3);

    private int value;
    private static Map map = new HashMap<>();

    private PacketType(int value) {
        this.value = value;
    }

    static {
        for (PacketType pageType : PacketType.values()) {
            map.put(pageType.value, pageType);
        }
    }

    public static PacketType valueOf(int pageType) {
        return (PacketType) map.get(pageType);
    }

    public int getValue() {
        return value;
    }
}
