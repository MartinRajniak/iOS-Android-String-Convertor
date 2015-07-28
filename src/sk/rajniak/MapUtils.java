package sk.rajniak;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MapUtils {

    public static Map<String, String> swapMap(Map<String, String> iOsBaseStringsMap) {
        final Map<String, String> swappedMap = new HashMap<>(iOsBaseStringsMap.size());
        for (Map.Entry<String, String> entry : iOsBaseStringsMap.entrySet()) {
            swappedMap.put(entry.getValue(), entry.getKey());
        }
        return swappedMap;
    }

    private static void printMap(Map iOsBaseStringsMap) {
        System.out.println("Name:" + "\t" + "Value:");
        int size = iOsBaseStringsMap.size();
        final Iterator keys = iOsBaseStringsMap.keySet().iterator();
        final Iterator values = iOsBaseStringsMap.values().iterator();
        for (int pos = 0; pos < size; pos++) {
            System.out.println(keys.next() + "\t" + values.next());
        }
    }
}
