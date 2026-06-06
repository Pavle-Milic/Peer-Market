package mutex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Stringifyer {

    public static String stringifyMap(Map<Integer, Integer> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        return map.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(","));
    }
    public static Map<Integer, Integer> destringifyMap(String str) {
        Map<Integer, Integer> map = new HashMap<>();
        if (str == null || str.trim().isEmpty()) {
            return map;
        }

        String[] pairs = str.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                map.put(Integer.parseInt(kv[0].trim()), Integer.parseInt(kv[1].trim()));
            }
        }
        return map;
    }
    public static String stringifyList(List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
    public static ArrayList<Integer> destringifyList(String str) {
        ArrayList<Integer> list = new ArrayList<>();
        if (str == null || str.trim().isEmpty()) {
            return list;
        }

        String[] parts = str.split(",");
        for (String part : parts) {
            list.add(Integer.parseInt(part.trim()));
        }
        return list;
    }
}