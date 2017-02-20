package me.vickychijwani.spectre.error;

public class UrlNotFoundException extends RuntimeException {

    /**
     * @param url - the URL that could not be found
     */
    public UrlNotFoundException(String url) {
        super("URL RETURNED 404: " + url);
    }

}
