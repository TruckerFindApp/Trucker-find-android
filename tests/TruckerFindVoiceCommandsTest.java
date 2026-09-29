package co.median.android;

public final class TruckerFindVoiceCommandsTest {
    private static void expect(String phrase, String query) {
        String actual = TruckerFindVoiceCommands.search(phrase);
        if (!java.util.Objects.equals(query, actual)) throw new AssertionError(phrase + ": " + actual);
    }
    public static void main(String[] args) {
        expect("Find fuel", "Fuel near me");
        expect("Show me food nearby", "Food near me");
        expect("Find truck parking", "Truck parking near me");
        expect("Find the nearest mechanic", "Truck repair near me");
        expect("Find laundry near me", "Laundry near me");
        expect("Truck wash", "Truck wash near me");
        expect("Hotels", "Hotels near me");
        expect("24/7", "24 hour truck stop near me");
        expect("Please find chrome shops", "Truck chrome shop near me");
        expect("Hey Trucker Find, weigh stations", "Truck weigh station near me");
        expect("Go to saved area Fuel", null);
        expect("Go to Home", null);
        expect("not fuel", null);
        expect("", null);
        expect(null, null);
        System.out.println("15 quick-search voice command checks passed.");
    }
}
