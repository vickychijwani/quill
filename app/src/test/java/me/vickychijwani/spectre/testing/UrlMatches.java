package me.vickychijwani.spectre.testing;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Custom Hamcrest matcher that matches 2 URLs ignoring ONLY trailing slashes. This can't handle
 * stuff like port 80/443 being the default for HTTP/HTTPS, or query params being unordered, or even
 * URLs being case-insensitive.
 */
public class UrlMatches extends TypeSafeMatcher<String> {

    private final String expected;

    public UrlMatches(String expected) {
        this.expected = expected;
    }

    @Override
    protected boolean matchesSafely(String actual) {
        return matchUrls(actual, expected);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("a URL string that exactly matches ")
                .appendValue(expected)
                .appendText(" ignoring trailing slashes");
    }

    private boolean matchUrls(String actual, String expected) {
        String normalizedActual = actual.replaceFirst("/$", "");
        String normalizedExpected = expected.replaceFirst("/$", "");
        return normalizedActual.equals(normalizedExpected);
    }

    @Factory
    public static Matcher<String> urlMatches(String expectedUrl) {
        return new UrlMatches(expectedUrl);
    }

}
