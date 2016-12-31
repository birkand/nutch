package tr.com.portos.nutch.parse.haber;

import java.util.regex.Pattern;

/**
 * Created by birkan on 27.12.2016.
 */
public class UrlCssSelector {
    Pattern urlPattern;
    String cssSelector;

    public UrlCssSelector(Pattern urlPattern, String cssSelector) {
        this.urlPattern = urlPattern;
        this.cssSelector = cssSelector;
    }
}
