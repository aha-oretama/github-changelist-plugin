package aha.oretama.jp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author sekineyasufumi on 2017/04/17.
 */
public class RegexUtils {

    public static List<String> createRegexps(List<String> changelist, String regex, String testTargetRegex) {
        Pattern pattern = Pattern.compile(regex);

        List<String> regexps = new ArrayList<>();
        for (String change : changelist) {
            Matcher matcher = pattern.matcher(change);

            if (matcher.find() && matcher.groupCount() >= 1) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if(matcher.group(i) != null) {
                        regexps.add(testTargetRegex.replace("$" + i, matcher.group(i)));
                    }
                }
            }
        }

        return regexps;
    }

}
