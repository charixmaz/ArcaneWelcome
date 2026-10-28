package dev.arcane.welcome;

import java.util.Optional;
import java.util.regex.Pattern;

public final class WelcomeText {
    private WelcomeText() {}
    public static Optional<String> target(String message, String word) {
        var matcher = Pattern.compile("(?i)^" + Pattern.quote(word)
                + "\\s+@?([A-Za-z0-9_]{1,16})$").matcher(message.strip());
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
