package co.median.android;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Exact category matching avoids turning an unrecognized destination into a different search. */
final class TruckerFindVoiceCommands {
    private static final Map<String, String> SEARCHES = new LinkedHashMap<>();
    static {
        add("Fuel", "fuel", "diesel", "gas", "gas station", "gas stations");
        add("Food", "food", "restaurant", "restaurants", "something to eat");
        add("Truck parking", "parking", "truck parking", "parking lot");
        add("Truck repair", "repair", "truck repair", "mechanic", "mechanics");
        add("Laundry", "laundry", "laundromat", "laundromats");
        add("Truck wash", "truck wash", "truck washes", "wash");
        add("Hotels", "hotel", "hotels", "motel", "motels");
        add("24 hour truck stop", "24 7", "24 hour", "24 hour truck stop", "24 hour truck stops", "twenty four seven", "twenty four hour truck stop", "truck stop", "truck stops");
        add("Truck chrome shop", "chrome", "chrome shop", "chrome shops", "truck chrome shop");
        add("Truck weigh station", "weigh station", "weigh stations", "weight station", "weight stations", "truck weigh station", "scales", "scale");
    }
    private static void add(String query, String... aliases) {
        for (String alias : aliases) SEARCHES.put(alias, query + " near me");
    }
    static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", " ").trim().replaceAll("\\s+", " ");
    }
    static String search(String speech) {
        String text = normalize(speech);
        text = text.replaceFirst("^please ", "").replaceFirst(" please$", "");
        text = text.replaceFirst("^(?:trucker find |hey trucker find )", "");
        text = text.replaceFirst("^(?:find me |find |search for |search |show me |show |look for |take me to |navigate to |go to )", "");
        text = text.replaceFirst("^(?:the nearest |nearest |nearby |a |an |the )", "");
        text = text.replaceFirst(" (?:near me|nearby|around me)$", "");
        return SEARCHES.get(text);
    }
    private TruckerFindVoiceCommands() { }
}
