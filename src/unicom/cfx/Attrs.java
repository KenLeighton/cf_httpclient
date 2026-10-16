package unicom.cfx;

import com.allaire.cfx.Request;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A case-insensitive copy of a CFX tag's attributes (the Request object is only valid during processRequest). */
final class Attrs {
    private final Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private static final Pattern NUMBER = Pattern.compile("^[-+]?(\\d+(\\.\\d*)?|\\.\\d+)");

    static Attrs of(Request request) {
        Attrs a = new Attrs();
        String[] names = request.getAttributeList();
        if (names != null) {
            for (String n : names) {
                if (n != null) { a.map.put(n, request.getAttribute(n)); }
            }
        }
        return a;
    }

    Attrs copyWithout(String name) {
        Attrs a = new Attrs();
        a.map.putAll(map);
        a.map.remove(name);
        return a;
    }

    boolean has(String name) { return map.get(name) != null; }

    /** The value as given (not trimmed), "" if missing - like toString(attributes.x) in CFML. */
    String raw(String name) {
        String v = map.get(name);
        return v == null ? "" : v;
    }

    /** The trimmed value, or dflt if the attribute is missing - like ga() in the CFML versions. */
    String get(String name, String dflt) {
        String v = map.get(name);
        return v == null ? dflt : v.trim();
    }

    String get(String name) { return get(name, ""); }

    /** "Y" (any case) */
    boolean yes(String name) { return "Y".equalsIgnoreCase(get(name)); }

    /** CFML val(): the leading number of the string, 0 if there is none. */
    static long val(String s) {
        if (s == null) { return 0; }
        Matcher m = NUMBER.matcher(s.trim());
        if (!m.find()) { return 0; }
        try { return (long) Double.parseDouble(m.group()); } catch (NumberFormatException e) { return 0; }
    }
}
