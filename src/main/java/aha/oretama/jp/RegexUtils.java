package aha.oretama.jp;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author aha-oretama.
 */
public class RegexUtils {

    public static List<String> createRegexps(List<String> changelist, String regex, String testTargetRegex) {
        Pattern pattern = Pattern.compile(regex);

        List<String> regexps = new ArrayList<>();
        for (String change : changelist) {
            Matcher matcher = pattern.matcher(change);

            if (matcher.find() && matcher.groupCount() >= 1) {
                boolean replaced = false;
                String tmp = testTargetRegex;
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if (tmp.contains("$" + i)) {
                        replaced = true;
                        tmp = tmp.replace("$" + i, StringUtils.defaultString(matcher.group(i)));
                    }
                }
                if (replaced) {
                    regexps.add(tmp);
                }
            }
        }

        return regexps;
    }

}
