package aha.oretama.jp;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
/**
 * @author sekineyasufumi on 2017/04/17.
 */
public class RegexUtilsTest {

    private static final List changelist = new ArrayList();

    // Default
    private String regex = "([^/]*?)(Test)?\\..*$";
    // Default
    private String testTargetRegex = "$1Test";


    static {
        changelist.add("src/java/util.java");
        changelist.add("src/java/groovyUtil.groovy");
        changelist.add("src/java/testUtilTest.groovy");
    }

    @Test
    public void createRegexps() throws Exception {

        List resultList = RegexUtils.createRegexps(changelist, regex, testTargetRegex);

        assertEquals("utilTest",resultList.get(0));
        assertEquals("groovyUtilTest",resultList.get(1));
        assertEquals("testUtilTest",resultList.get(2));
    }

}
