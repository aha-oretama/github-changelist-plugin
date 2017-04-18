package aha.oretama.jp;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
/**
 * @author aha-oretama.
 */
public class RegexUtilsTest {

    private static final List changelist = new ArrayList();

    // Default
    private String regex = "([^/]*?)(Test)?(\\..*)?$";
    // Default
    private String testTargetRegex = "**/$1Test*";


    static {
        changelist.add("src/java/util.java");
        changelist.add("src/java/groovyUtil.groovy");
        changelist.add("src/java/testUtilTest.java");
        changelist.add("src/java/noExtension");
    }

    @Test
    public void createRegexps_defaultParam() throws Exception {

        List resultList = RegexUtils.createRegexps(changelist, regex, testTargetRegex);

        assertEquals("**/utilTest*",resultList.get(0));
        assertEquals("**/groovyUtilTest*",resultList.get(1));
        assertEquals("**/testUtilTest*",resultList.get(2));
        assertEquals("**/noExtensionTest*",resultList.get(3));
    }

    @Test
    public void createRegexps_extractFileName() throws Exception {
        regex = "([^/]*$)";
        testTargetRegex = "$1";

        List resultList = RegexUtils.createRegexps(changelist, regex, testTargetRegex);

        assertEquals("util.java",resultList.get(0));
        assertEquals("groovyUtil.groovy",resultList.get(1));
        assertEquals("testUtilTest.java",resultList.get(2));
        assertEquals("noExtension",resultList.get(3));
    }

    @Test
    public void createRegexps_multigroups() throws Exception {
        testTargetRegex = "$1Test$3";

        List resultList = RegexUtils.createRegexps(changelist, regex, testTargetRegex);

        assertEquals("utilTest.java",resultList.get(0));
        assertEquals("groovyUtilTest.groovy",resultList.get(1));
        assertEquals("testUtilTest.java",resultList.get(2));
        assertEquals("noExtensionTest",resultList.get(3));
    }

}
