// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.util.lang;

import consulo.util.lang.internal.NaturalComparator;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Eugene Zhuravlev
 * @author UNV
 */
public class StringUtilTest {
    @Test
    void testAreSameInstance() {
        assertThat(StringUtil.areSameInstance(null, null)).isTrue();
        String foo = "foo";
        assertThat(StringUtil.areSameInstance(foo, foo)).isTrue();
        assertThat(StringUtil.areSameInstance(foo, foo.toUpperCase().toLowerCase())).isFalse();
    }

    @Test
    void testCommonPrefix() {
        assertThat(StringUtil.commonPrefix("foo", "bar")).isEqualTo("");
        assertThat(StringUtil.commonPrefix("foo", "foobar")).isEqualTo("foo");
    }

    @Test
    void testCommonPrefixLength() {
        assertThat(StringUtil.commonPrefixLength("foo", "bar")).isEqualTo(0);
        assertThat(StringUtil.commonPrefixLength("foo", "foobar")).isEqualTo(3);
    }

    @Test
    void testCommonSuffix() {
        assertThat(StringUtil.commonSuffix("foo", "bar")).isEqualTo("");
        assertThat(StringUtil.commonSuffix("bar", "foobar")).isEqualTo("bar");
    }

    @Test
    void testCommonSuffixLength() {
        assertThat(StringUtil.commonSuffixLength("foo", "bar")).isEqualTo(0);
        assertThat(StringUtil.commonSuffixLength("bar", "foobar")).isEqualTo(3);
    }

    @Test
    void testCompareStrings() {
        assertThat(StringUtil.compare(null, null, false)).isEqualTo(0);
        assertThat(StringUtil.compare("foobar", null, false)).isGreaterThan(0);
        assertThat(StringUtil.compare(null, "foobar", false)).isLessThan(0);
        assertThat(StringUtil.compare("foobar", "foobar", false)).isEqualTo(0);
        assertThat(StringUtil.compare("foobar", "Foobar", false)).isGreaterThan(0);
        assertThat(StringUtil.compare("foobar", "Foobar", true)).isEqualTo(0);
        assertThat(StringUtil.compare("foobar", "f00bar", false)).isGreaterThan(0);
        assertThat(StringUtil.compare("foobar", "foo", false)).isGreaterThan(0);
    }

    @Test
    void testCompareCharSequences() {
        assertThat(StringUtil.compare((CharSequence) null, null, false)).isEqualTo(0);
        assertThat(StringUtil.compare((CharSequence) "foobar", null, false)).isGreaterThan(0);
        assertThat(StringUtil.compare((CharSequence) null, "foobar", false)).isLessThan(0);
        assertThat(StringUtil.compare((CharSequence) "foobar", "foobar", false)).isEqualTo(0);
        assertThat(StringUtil.compare((CharSequence) "foobar", "Foobar", false)).isGreaterThan(0);
        assertThat(StringUtil.compare((CharSequence) "foobar", "Foobar", true)).isEqualTo(0);
        assertThat(StringUtil.compare((CharSequence) "foobar", "f00bar", false)).isGreaterThan(0);
        assertThat(StringUtil.compare((CharSequence) "foobar", "foo", false)).isGreaterThan(0);
    }

    @Test
    void testContains() {
        assertTrue(StringUtil.contains("1", "1"));
        assertFalse(StringUtil.contains("1", "12"));
        assertTrue(StringUtil.contains("12", "1"));
        assertTrue(StringUtil.contains("12", "2"));
    }

    @Test
    void testContainsIgnoreCase() {
        assertThat(StringUtil.containsIgnoreCase("Foobar", "foo")).isTrue();
        assertThat(StringUtil.containsIgnoreCase("Foobar", "bar")).isTrue();
        assertThat(StringUtil.containsIgnoreCase("Foobar", "qux")).isFalse();
    }

    @Test
    void testContainsLineBreak() {
        assertThat(StringUtil.containsLineBreak("foobar")).isFalse();
        assertThat(StringUtil.containsLineBreak("foo\nbar")).isTrue();
    }

    @Test
    void testContainsWhitespaces() {
        assertThat(StringUtil.containsWhitespaces(null)).isFalse();
        assertThat(StringUtil.containsWhitespaces("foobar")).isFalse();
        assertThat(StringUtil.containsWhitespaces("foo bar")).isTrue();
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testCountChars() {
        assertThat(StringUtil.countChars("abcdefgh", 'x')).isEqualTo(0);
        assertThat(StringUtil.countChars("abcdefgh", 'd')).isEqualTo(1);
        assertThat(StringUtil.countChars("abcddddefghd", 'd')).isEqualTo(5);
        assertThat(StringUtil.countChars("abcddddefghd", 'd', 4, false)).isEqualTo(4);
        assertThat(StringUtil.countChars("abcddddefghd", 'd', 4, true)).isEqualTo(3);
        assertThat(StringUtil.countChars("abcddddefghd", 'd', 4, 6, false)).isEqualTo(2);
        assertThat(StringUtil.countChars("aaabcddddefghdaaaa", 'a', -20, 20, true)).isEqualTo(3);
        assertThat(StringUtil.countChars("aaabcddddefghdaaaa", 'a', 20, -20, true)).isEqualTo(4);
    }

    @Test
    void testCountNewLines() {
        assertThat(StringUtil.countNewLines("\n foo \n bar \n")).isEqualTo(3);
    }

    @Test
    void testEndsWith() {
        assertTrue(StringUtil.endsWith("text", 0, 4, "text"));
        assertFalse(StringUtil.endsWith("text", 4, 4, "-->"));
        assertThatThrownBy(() -> StringUtil.endsWith("text", -1, 4, "t"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid offsets: start=-1; end=4; text.length()=4");
        assertFalse(StringUtil.endsWith("text", "-->"));
    }

    @Test
    void testEndsWithLineBreak() {
        assertThat(StringUtil.endsWithLineBreak("foo")).isFalse();
        assertThat(StringUtil.endsWithLineBreak("foo\n")).isTrue();
        assertThat(StringUtil.endsWithLineBreak("foo\r")).isTrue();
    }

    @Test
    void testEqual() {
        assertThat(StringUtil.equal(null, null, true)).isTrue();
        assertThat(StringUtil.equal("foobar", null, true)).isFalse();
        assertThat(StringUtil.equal(null, "foobar", true)).isFalse();
        assertThat(StringUtil.equal("foobar", "foobar", true)).isTrue();
        assertThat(StringUtil.equal("foobar", "Foobar", true)).isFalse();
        assertThat(StringUtil.equal("foobar", "Foobar", false)).isTrue();
        assertThat(StringUtil.equal("foobar", "f00bar", true)).isFalse();
        assertThat(StringUtil.equal("foobar", "f00bar", false)).isFalse();
        assertThat(StringUtil.equal("foobar", "foo", true)).isFalse();
    }

    @Test
    void testEquals() {
        assertThat(StringUtil.equals(null, null)).isTrue();
        assertThat(StringUtil.equals("foobar", null)).isFalse();
        assertThat(StringUtil.equals(null, "foobar")).isFalse();
        assertThat(StringUtil.equals("foobar", "foobar")).isTrue();
        assertThat(StringUtil.equals("foobar", "f00bar")).isFalse();
        assertThat(StringUtil.equals("foobar", "foo")).isFalse();
    }

    @Test
    void testEqualsIgnoreCase() {
        assertThat(StringUtil.equalsIgnoreCase(null, null)).isTrue();
        assertThat(StringUtil.equalsIgnoreCase("foobar", null)).isFalse();
        assertThat(StringUtil.equalsIgnoreCase(null, "foobar")).isFalse();
        assertThat(StringUtil.equalsIgnoreCase("foobar", "foobar")).isTrue();
        assertThat(StringUtil.equalsIgnoreCase("foobar", "Foobar")).isTrue();
        assertThat(StringUtil.equalsIgnoreCase("foobar", "f00bar")).isFalse();
        assertThat(StringUtil.equalsIgnoreCase("foobar", "foo")).isFalse();
    }

    @Test
    void testEqualsIgnoreWhitespaces() {
        assertTrue(StringUtil.equalsIgnoreWhitespaces(null, null));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("", null));

        assertTrue(StringUtil.equalsIgnoreWhitespaces("", ""));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("\n\t ", ""));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("", "\t\n \n\t"));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("\t", "\n"));

        assertTrue(StringUtil.equalsIgnoreWhitespaces("x", " x"));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("x", "x "));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("x\n", "x"));

        assertTrue(StringUtil.equalsIgnoreWhitespaces("abc", "a\nb\nc\n"));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("x y x", "x y x"));
        assertTrue(StringUtil.equalsIgnoreWhitespaces("xyx", "x y x"));

        assertFalse(StringUtil.equalsIgnoreWhitespaces("x", "\t\n "));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("", " x "));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("", "x "));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("", " x"));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("xyx", "xxx"));
        assertFalse(StringUtil.equalsIgnoreWhitespaces("xyx", "xYx"));
    }

    @Test
    void testEqualsTrimWhitespaces() {
        assertThat(StringUtil.equalsTrimWhitespaces(" \tfoo\n", "foo")).isTrue();
        assertThat(StringUtil.equalsTrimWhitespaces("foo", " \tfoo\n")).isTrue();
        assertThat(StringUtil.equalsTrimWhitespaces("foo", "Foo")).isFalse();
    }

    @Test
    void testEscapeBackSlashes() {
        assertThat(StringUtil.escapeBackSlashes("\\\\server\\share\\extension.crx")).isEqualTo("\\\\\\\\server\\\\share\\\\extension.crx");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testEscapeCharCharacters() {
        assertThat(StringUtil.escapeCharCharacters("\0\b\f\n\r\t\u007F\"'\\foo")).isEqualTo("\\u0000\\b\\f\\n\\r\\t\\u007F\"\\'\\\\foo");
    }

    @Test
    void testEscapeLineBreak() {
        assertThat(StringUtil.escapeLineBreak("foo\r\n")).isEqualTo("foo\\r\\n");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testEscapeMnemonics() {
        assertThat(StringUtil.escapeMnemonics(null)).isNull();
        assertThat(StringUtil.escapeMnemonics("& _")).isEqualTo("&& __");
    }

    @Test
    void testEscapePattern() {
        assertThat(StringUtil.escapePattern("{ '")).isEqualTo("'{' ''");
        assertThat(StringUtil.escapePattern("'{")).isEqualTo("'''{'");
    }

    @Test
    void testEscapeProperty() {
        assertThat(StringUtil.escapeProperty(" foo \t\r\n\f\u0420#!:=\\", true)).isEqualTo("\\ foo \\t\\r\\n\\f\\u0420\\#\\!\\:\\=\\\\");
        assertThat(StringUtil.escapeProperty(" foo \t\r\n\f\u0420#!:=\\", false)).isEqualTo(" foo \\t\\r\\n\\f\\u0420\\#\\!\\:\\=\\\\");
    }

    @Test
    void testEscapeQuotesString() {
        assertThat(StringUtil.escapeQuotes("\"")).isEqualTo("\\\"");
        assertThat(StringUtil.escapeQuotes("foo\"bar'\"")).isEqualTo("foo\\\"bar'\\\"");
    }

    @Test
    void testEscapeQuotesStringBuilder() {
        StringBuilder sb = sb("\"");
        StringUtil.escapeQuotes(sb);
        assertThat(sb).hasToString("\\\"");

        sb = sb("foo\"bar'\"");
        StringUtil.escapeQuotes(sb);
        assertThat(sb).hasToString("foo\\\"bar'\\\"");
    }

    @Test
    void testEscapeSlashesString() {
        assertThat(StringUtil.escapeSlashes("/")).isEqualTo("\\/");
        assertThat(StringUtil.escapeSlashes("foo/bar\\foo/")).isEqualTo("foo\\/bar\\foo\\/");
    }

    @Test
    void testEscapeSlashesStringBuilder() {
        StringBuilder sb = sb("/");
        StringUtil.escapeSlashes(sb);
        assertThat(sb).hasToString("\\/");

        sb = sb("foo/bar\\foo/");
        StringUtil.escapeSlashes(sb);
        assertThat(sb).hasToString("foo\\/bar\\foo\\/");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testEscapeStringCharacters() {
        assertThat(StringUtil.escapeStringCharacters("\b\f\n\r\t\u007F\"'\\foo")).isEqualTo("\\b\\f\\n\\r\\t\\u007F\\\"'\\\\foo");

        StringBuilder sb = sb();
        StringUtil.escapeStringCharacters(12, "\b\f\n\r\t\u007F\"'\\foo", sb);
        assertThat(sb).hasToString("\\b\\f\\n\\r\\t\\u007F\\\"'\\\\foo");

        assertThat(StringUtil.escapeStringCharacters(3, "\\\"\n", "\"", false, sb())).hasToString("\\\"\\n");
        assertThat(StringUtil.escapeStringCharacters(2, "\"\n", "\"", false, sb())).hasToString("\\\"\\n");
    }

    @Test
    void testEscapeToRegexp() {
        assertThat(StringUtil.escapeToRegexp("foo.$|()[]{}^?*+\\\r\n"))
            .isEqualTo("foo\\.\\$\\|\\(\\)\\[\\]\\{\\}\\^\\?\\*\\+\\\\\\r\\n");
    }

    @SuppressWarnings({"ConstantConditions", "deprecation"})
    @Test
    void testEscapeXml() {
        assertThat(StringUtil.escapeXml(null)).isNull();
        assertThat(StringUtil.escapeXml("<&'\">")).isEqualTo("&lt;&amp;&#39;&quot;&gt;");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testEscapeXmlEntities() {
        assertThat(StringUtil.escapeXmlEntities("<&'\">")).isEqualTo("&lt;&amp;&#39;&quot;&gt;");
    }

    @Test
    void testFilterEmptyStrings() {
        String[] nonEmpty = {"foo", "bar"};
        assertThat(StringUtil.filterEmptyStrings(nonEmpty)).isSameAs(nonEmpty);
        assertThat(StringUtil.filterEmptyStrings(new String[]{"foo", null, "", "bar"})).containsExactly("foo", "bar");
    }

    @Test
    void testFindMatches() {
        assertThat(StringUtil.findMatches("foobar bar", Pattern.compile("(\\w*)(bar)"))).containsExactly("foo", "");
        assertThat(StringUtil.findMatches("foobar bar", Pattern.compile("(\\w*)(bar)"), 2)).containsExactly("bar", "bar");
    }

    @Test
    void testFirstCharSequence() {
        StringBuilder foo = sb("foo");
        assertThat(StringUtil.first(foo, 3, false)).isSameAs(foo);
        assertThat(StringUtil.first(foo, 3, true)).isSameAs(foo);
        assertThat(StringUtil.first(sb("foobar"), 3, false)).hasToString("foo");
        assertThat(StringUtil.first(sb("foobar"), 3, true)).hasToString("foo...");
    }

    @Test
    void testFirstString() {
        String foo = "foo";
        assertThat(StringUtil.first(foo, 3, false)).isSameAs(foo);
        assertThat(StringUtil.first(foo, 3, true)).isSameAs(foo);
        assertThat(StringUtil.first("foobar", 3, false)).isEqualTo("foo");
        assertThat(StringUtil.first("foobar", 3, true)).isEqualTo("foo...");
    }

    @Test
    void testFixVariableNameDerivedFromPropertyName() {
        assertThat(StringUtil.fixVariableNameDerivedFromPropertyName("")).isEqualTo("");
        assertThat(StringUtil.fixVariableNameDerivedFromPropertyName(" ")).isEqualTo(" ");
        assertThat(StringUtil.fixVariableNameDerivedFromPropertyName("foo")).isEqualTo("aFoo");
        assertThat(StringUtil.fixVariableNameDerivedFromPropertyName("oof")).isEqualTo("anOof");
    }

    @Test
    void testFormatDuration() {
        assertThat(StringUtil.formatDuration(0)).isEqualTo("0 ms");
        assertThat(StringUtil.formatDuration(1)).isEqualTo("1 ms");
        assertThat(StringUtil.formatDuration(1000)).isEqualTo("1 s");
        assertThat(StringUtil.formatDuration(Integer.MAX_VALUE)).isEqualTo("24 d 20 h 31 m 23 s 647 ms");
        assertThat(StringUtil.formatDuration(Integer.MAX_VALUE + 5000000000L)).isEqualTo("82 d 17 h 24 m 43 s 647 ms");

        assertThat(StringUtil.formatDuration(60100)).isEqualTo("1 m 0 s 100 ms");

        assertThat(StringUtil.formatDuration(1234)).isEqualTo("1 s 234 ms");
        assertThat(StringUtil.formatDuration(12345)).isEqualTo("12 s 345 ms");
        assertThat(StringUtil.formatDuration(123456)).isEqualTo("2 m 3 s 456 ms");
        assertThat(StringUtil.formatDuration(1234567)).isEqualTo("20 m 34 s 567 ms");
        assertThat(StringUtil.formatDuration(12345678)).isEqualTo("3 h 25 m 45 s 678 ms");
        assertThat(StringUtil.formatDuration(123456789)).isEqualTo("1 d 10 h 17 m 36 s 789 ms");
        assertThat(StringUtil.formatDuration(1234567890)).isEqualTo("14 d 6 h 56 m 7 s 890 ms");

        assertThat(StringUtil.formatDuration(3378606101L)).isEqualTo("39 d 2 h 30 m 6 s 101 ms");
    }

    @Test
    void testFormatFileSize() {
        assertThatThrownBy(() -> StringUtil.formatFileSize(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Invalid value: -1");

        assertFileSizeFormat(0, "0 B");
        assertFileSizeFormat(1, "1 B");
        assertFileSizeFormat(Integer.MAX_VALUE, "2.15 GB");
        assertFileSizeFormat(Long.MAX_VALUE, "9.22 EB");

        assertFileSizeFormat(60_100, "60.1 kB");

        assertFileSizeFormat(1_234, "1.23 kB");
        assertFileSizeFormat(12_345, "12.35 kB");
        assertFileSizeFormat(123_456, "123.46 kB");
        assertFileSizeFormat(1_234_567, "1.23 MB");
        assertFileSizeFormat(12_345_678, "12.35 MB");
        assertFileSizeFormat(123_456_789, "123.46 MB");
        assertFileSizeFormat(1_234_567_890, "1.23 GB");

        assertFileSizeFormat(999, "999 B");
        assertFileSizeFormat(1000, "1 kB");
        assertFileSizeFormat(999_994, "999.99 kB");
        assertFileSizeFormat(999_995, "1 MB");
        assertFileSizeFormat(999_994_999, "999.99 MB");
        assertFileSizeFormat(999_995_000, "1 GB");
        assertFileSizeFormat(999_994_999_999L, "999.99 GB");
        assertFileSizeFormat(999_995_000_000L, "1 TB");
    }

    private void assertFileSizeFormat(long sizeBytes, String expectedFormatted) {
        assertThat(StringUtil.formatFileSize(sizeBytes)).isEqualTo(expectedFormatted);
    }

    @Test
    void testGetLineBreakCount() {
        assertThat(StringUtil.getLineBreakCount("\rfoo\r\nbar\nqux")).isEqualTo(3);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testGetOccurrenceCount() {
        assertThat(StringUtil.getOccurrenceCount("aabb", 'a')).isEqualTo(2);
        assertThat(StringUtil.getOccurrenceCount("ogogogogo", "ogo")).isEqualTo(4);
    }

    @Test
    void testGetPackageName() {
        assertThat(StringUtil.getPackageName("java.lang.String")).isEqualTo("java.lang");
        assertThat(StringUtil.getPackageName("java.util.Map.Entry")).isEqualTo("java.util.Map");
        assertThat(StringUtil.getPackageName("Map.Entry")).isEqualTo("Map");
        assertThat(StringUtil.getPackageName("Number")).isEqualTo("");
    }

    @Test
    void testGetPropertyName() {
        assertThat(StringUtil.getPropertyName("getFooBar")).isEqualTo("fooBar");
        assertThat(StringUtil.getPropertyName("isFooBar")).isEqualTo("fooBar");
        assertThat(StringUtil.getPropertyName("setFooBar")).isEqualTo("fooBar");
        assertThat(StringUtil.getPropertyName("fooBar")).isNull();
    }

    @Test
    void testGetQualifiedName() {
        assertThat(StringUtil.getQualifiedName(null, "Bar")).isEqualTo("Bar");
        assertThat(StringUtil.getQualifiedName("", "Bar")).isEqualTo("Bar");
        assertThat(StringUtil.getQualifiedName("foo", "Bar")).isEqualTo("foo.Bar");
    }

    @Test
    void testGetShortName() {
        assertThat(StringUtil.getShortName(String.class)).isEqualTo("String");
        assertThat(StringUtil.getShortName("java.lang.String")).isEqualTo("String");
        assertThat(StringUtil.getShortName("String")).isEqualTo("String");
        assertThat(StringUtil.getShortName("java/lang/String", '/')).isEqualTo("String");
    }

    @Test
    void testGetWordsIn() {
        assertThat(StringUtil.getWordsIn("")).isEmpty();
        assertThat(StringUtil.getWordsIn("f")).containsExactly("f");
        assertThat(StringUtil.getWordsIn("fooBar baz")).containsExactly("fooBar", "baz");
        assertThat(StringUtil.getWordsIn("fooBar#baz")).containsExactly("fooBar", "baz");
    }

    @Test
    void testGetWordsInStringLongestFirst() {
        assertThat(StringUtil.getWordsInStringLongestFirst("")).isEmpty();
        assertThat(StringUtil.getWordsInStringLongestFirst("baz fooBar")).containsExactly("fooBar", "baz");
    }

    @Test
    void testGetWordsWithOffset() {
        assertThat(StringUtil.getWordsWithOffset("foo bar")).containsExactly(Pair.create("foo", 0), Pair.create("bar", 4));
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testHasLowerCaseChar() {
        assertThat(StringUtil.hasLowerCaseChar("FOOBAR")).isFalse();
        assertThat(StringUtil.hasLowerCaseChar("fOOBAR")).isTrue();
    }

    @Test
    void testHasUpperCaseChar() {
        assertThat(StringUtil.hasUpperCaseChar("foobar")).isFalse();
        assertThat(StringUtil.hasUpperCaseChar("Foobar")).isTrue();
    }

    @Test
    void testIndexOf_1() {
        char[] chars = new char[]{'a', 'b', 'c', 'd', 'a', 'b', 'c', 'd', 'A', 'B', 'C', 'D'};
        assertThat(StringUtil.indexOf(chars, 'c', 0, 12, false)).isEqualTo(2);
        assertThat(StringUtil.indexOf(chars, 'C', 0, 12, false)).isEqualTo(2);
        assertThat(StringUtil.indexOf(chars, 'C', 0, 12, true)).isEqualTo(10);
        assertThat(StringUtil.indexOf(chars, 'c', -42, 99, false)).isEqualTo(2);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testIndexOf_2() {
        assertThat(StringUtil.indexOf("axaxa", 'x', 0, 5)).isEqualTo(1);
        assertThat(StringUtil.indexOf("abcd", 'c', -42, 99)).isEqualTo(2);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testIndexOf_3() {
        assertThat(StringUtil.indexOf("axaXa", 'x', 0, 5, false)).isEqualTo(1);
        assertThat(StringUtil.indexOf("axaXa", 'X', 0, 5, true)).isEqualTo(3);
        assertThat(StringUtil.indexOf("abcd", 'c', -42, 99, false)).isEqualTo(2);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testIndexOfAny() {
        assertThat(StringUtil.indexOfAny("axxa", "")).isEqualTo(-1);
        assertThat(StringUtil.indexOfAny("axxa", "x")).isEqualTo(1);
        assertThat(StringUtil.indexOfAny("axxa", "zx")).isEqualTo(1);
        assertThat(StringUtil.indexOfAny("axxa", "z")).isEqualTo(-1);
        assertThat(StringUtil.indexOfAny("abcd", "c", -42, 99)).isEqualTo(2);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testIndexOfAnyCharSequence() {
        assertThat(StringUtil.indexOfAny((CharSequence) "axa", "")).isEqualTo(-1);
        assertThat(StringUtil.indexOfAny((CharSequence) "axa", "x")).isEqualTo(1);
        assertThat(StringUtil.indexOfAny((CharSequence) "axa", "zx")).isEqualTo(1);
        assertThat(StringUtil.indexOfAny((CharSequence) "axa", "z")).isEqualTo(-1);
        assertThat(StringUtil.indexOfAny((CharSequence) "abcd", "c", -42, 99)).isEqualTo(2);
    }

    @Test
    void testIndexOfIgnoreCaseString() {
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", "foo", Integer.MIN_VALUE)).isEqualTo(0);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", "foo", -1)).isEqualTo(0);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", "foo", 0)).isEqualTo(0);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", "foo", 1)).isEqualTo(-1);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", "foo", 100)).isEqualTo(-1);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", "", -1)).isEqualTo(0);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", "", 0)).isEqualTo(0);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", "", 100)).isEqualTo(6);
    }

    @Test
    void testIndexOfIgnoreCaseChar() {
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", 'f', Integer.MIN_VALUE)).isEqualTo(0);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", 'f', -1)).isEqualTo(0);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", 'f', 0)).isEqualTo(0);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", 'f', 1)).isEqualTo(-1);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", 'f', Integer.MAX_VALUE)).isEqualTo(-1);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", 'z', -1)).isEqualTo(-1);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", 'z', 0)).isEqualTo(-1);
        assertThat(StringUtil.indexOfIgnoreCase("Foobar", 'z', Integer.MAX_VALUE)).isEqualTo(-1);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testInternEmptyString() {
        assertThat(StringUtil.internEmptyString(null)).isNull();

        String empty = new String(new char[0]);
        assertThat(StringUtil.internEmptyString(empty))
            .isNotSameAs(empty)
            .isSameAs("");

        String foo = "foo";
        assertThat(StringUtil.internEmptyString(foo)).isSameAs(foo);
    }

    @Test
    void testIsChar() {
        assertThat(StringUtil.isChar("foo", -1, 'f')).isFalse();
        assertThat(StringUtil.isChar("foo", 0, 'f')).isTrue();
        assertThat(StringUtil.isChar("foo", 0, 'o')).isFalse();
        assertThat(StringUtil.isChar("foo", 3, 'o')).isFalse();
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void testIsEmptyOrSpaces() {
        assertTrue(StringUtil.isEmptyOrSpaces(null));
        assertTrue(StringUtil.isEmptyOrSpaces(""));
        assertTrue(StringUtil.isEmptyOrSpaces("                   "));

        assertFalse(StringUtil.isEmptyOrSpaces("1"));
        assertFalse(StringUtil.isEmptyOrSpaces("         12345          "));
        assertFalse(StringUtil.isEmptyOrSpaces("test"));
    }

    @Test
    void testIsJavaIdentifierPart() {
        assertThat(StringUtil.isJavaIdentifierPart('a')).isTrue();
        assertThat(StringUtil.isJavaIdentifierPart('A')).isTrue();
        assertThat(StringUtil.isJavaIdentifierPart('\u0430')).isTrue();
        assertThat(StringUtil.isJavaIdentifierPart('0')).isTrue();
    }

    @Test
    void testIsJavaIdentifierStart() {
        assertThat(StringUtil.isJavaIdentifierStart('a')).isTrue();
        assertThat(StringUtil.isJavaIdentifierStart('A')).isTrue();
        assertThat(StringUtil.isJavaIdentifierStart('\u0430')).isTrue();
        assertThat(StringUtil.isJavaIdentifierStart('0')).isFalse();
    }

    @Test
    void testIsJavaIdentifier() {
        assertFalse(StringUtil.isJavaIdentifier(""));
        assertTrue(StringUtil.isJavaIdentifier("x"));
        assertFalse(StringUtil.isJavaIdentifier("0"));
        assertFalse(StringUtil.isJavaIdentifier("0x"));
        assertTrue(StringUtil.isJavaIdentifier("x0"));
        assertTrue(StringUtil.isJavaIdentifier("\uD835\uDEFCA"));
        assertTrue(StringUtil.isJavaIdentifier("A\uD835\uDEFC"));
        //noinspection UnnecessaryUnicodeEscape
        assertTrue(StringUtil.isJavaIdentifier("\u03B1A"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testIsNotEmpty() {
        assertThat(StringUtil.isNotEmpty(null)).isFalse();
        assertThat(StringUtil.isNotEmpty("")).isFalse();
        assertThat(StringUtil.isNotEmpty("foo")).isTrue();
    }

    @Test
    void testIsNotNegativeNumber() {
        assertThat(StringUtil.isNotNegativeNumber(null)).isFalse();
        assertThat(StringUtil.isNotNegativeNumber("")).isTrue(); //TODO: incorrect
        assertThat(StringUtil.isNotNegativeNumber("123")).isTrue();
        assertThat(StringUtil.isNotNegativeNumber("+123")).isFalse();
        assertThat(StringUtil.isNotNegativeNumber("-123")).isFalse();
        assertThat(StringUtil.isNotNegativeNumber("foo")).isFalse();
    }

    @SuppressWarnings("deprecation")
    @Test
    void testIsQuotedString() {
        assertFalse(StringUtil.isQuotedString(""));
        assertFalse(StringUtil.isQuotedString("'"));
        assertFalse(StringUtil.isQuotedString("\""));
        assertTrue(StringUtil.isQuotedString("\"\""));
        assertTrue(StringUtil.isQuotedString("''"));
        assertTrue(StringUtil.isQuotedString("'ab'"));
        assertTrue(StringUtil.isQuotedString("\"foo\""));
    }

    @SuppressWarnings("NullArgumentToVariableArgMethod")
    @Test
    void testJoinArray1() {
        assertThat(StringUtil.join(null)).isSameAs("");
        assertThat(StringUtil.join(new String[0])).isSameAs("");
        assertThat(StringUtil.join("foo")).isSameAs("foo");
        assertThat(StringUtil.join("foo", "bar")).isEqualTo("foobar");
    }

    @Test
    void testJoinArray2() {
        assertThat(StringUtil.join(new String[0], ","))
            .isEqualTo(StringUtil.join(new String[0], ",", sb()).toString())
            .isSameAs("");
        assertThat(StringUtil.join(new String[]{"foo"}, ","))
            .isEqualTo(StringUtil.join(new String[]{"foo"}, ",", sb()).toString())
            .isSameAs("foo");
        assertThat(StringUtil.join(new String[]{"foo", "", "bar"}, ","))
            .isEqualTo(StringUtil.join(new String[]{"foo", "", "bar"}, ",", sb()).toString())
            .isEqualTo("foo,,bar");
    }

    @Test
    void testJoinArrayFunction() {
        assertThat(StringUtil.join(new StringBuilder[]{}, StringBuilder::toString, ",")).isSameAs("");
        assertThat(StringUtil.join(new StringBuilder[]{sb("qqq")}, StringBuilder::toString, ",")).isEqualTo("qqq");
        assertThat(StringUtil.join(new StringBuilder[]{sb()}, StringBuilder::toString, ",")).isEmpty();
        assertThat(StringUtil.join(new StringBuilder[]{sb()}, sb -> null, ",")).isSameAs("");
        assertThat(StringUtil.join(new StringBuilder[]{sb("a"), sb("b")}, StringBuilder::toString, ",")).isEqualTo("a,b");
        assertThat(StringUtil.join(new StringBuilder[]{sb("foo"), sb(), sb("bar")}, StringBuilder::toString, ",")).isEqualTo("foo,bar");
    }

    @Test
    void testJoinCollection() {
        assertThat(StringUtil.join(List.of(), ",")).isSameAs("");
        assertThat(StringUtil.join(List.of("qqq"), ",")).isSameAs("qqq");
        assertThat(StringUtil.join(Collections.singletonList(null), ",")).isSameAs("");
        assertThat(StringUtil.join(List.of("a", "b"), ",")).isEqualTo("a,b");
        assertThat(StringUtil.join(List.of("foo", "", "bar"), ",")).isEqualTo("foo,,bar");
        assertThat(StringUtil.join(Arrays.asList("foo", null, "bar"), ",")).isEqualTo("foo,bar");
    }

    @Test
    void testJoinCollectionFunction() {
        assertThat(StringUtil.join(List.<StringBuilder>of(), StringBuilder::toString, ",")).isSameAs("");
        assertThat(StringUtil.join(List.of(sb("qqq")), StringBuilder::toString, ",")).isEqualTo("qqq");
        assertThat(StringUtil.join(List.of(sb()), StringBuilder::toString, ",")).isEmpty();
        assertThat(StringUtil.join(List.of(sb()), sb -> null, ",")).isSameAs("");
        assertThat(StringUtil.join(List.of(sb("a"), sb("b")), StringBuilder::toString, ",")).isEqualTo("a,b");
        assertThat(StringUtil.join(List.of(sb("foo"), sb(), sb("bar")), StringBuilder::toString, ",")).isEqualTo("foo,bar");
    }

    @Test
    void testJoinIterable() {
        assertThat(StringUtil.join((Iterable) List.of(), ",")).isEmpty();
        assertThat(StringUtil.join((Iterable) List.of("foo"), ",")).isEqualTo("foo");
        assertThat(StringUtil.join((Iterable) Collections.singletonList(null), ",")).isEqualTo("null"); // TODO: Seems inconsistent
        assertThat(StringUtil.join((Iterable) List.of("foo", "bar"), ",")).isEqualTo("foo,bar");
        assertThat(StringUtil.join((Iterable) List.of("foo", "", "bar"), ",")).isEqualTo("foo,,bar");
        assertThat(StringUtil.join((Iterable) Arrays.asList("foo", null, "bar"), ",")).isEqualTo("foo,null,bar");
    }

    @Test
    void testJoinIterableFunction() {
        assertThat(StringUtil.join((Iterable<StringBuilder>) List.<StringBuilder>of(), StringBuilder::toString, ",")).isEmpty();
        assertThat(StringUtil.join((Iterable<StringBuilder>) List.of(sb("qqq")), StringBuilder::toString, ",")).isEqualTo("qqq");
        assertThat(StringUtil.join((Iterable<StringBuilder>) List.of(sb()), StringBuilder::toString, ",")).isEmpty();
        assertThat(StringUtil.join((Iterable<StringBuilder>) List.of(sb()), sb -> null, ",")).isEmpty();
        assertThat(StringUtil.join((Iterable<StringBuilder>) List.of(sb("a"), sb("b")), StringBuilder::toString, ",")).isEqualTo("a,b");
        assertThat(StringUtil.join((Iterable<StringBuilder>) List.of(sb("foo"), sb(), sb("bar")), StringBuilder::toString, ","))
            .isEqualTo("foo,bar");
    }

    @Test
    void testLastCharSequence() {
        StringBuilder foo = sb("foo");
        assertThat(StringUtil.last(foo, 3, false)).isSameAs(foo);
        assertThat(StringUtil.last(foo, 3, true)).isSameAs(foo);
        assertThat(StringUtil.last(sb("foobar"), 3, false)).hasToString("bar");
        assertThat(StringUtil.last(sb("foobar"), 3, true)).hasToString("...bar");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testLastIndexOf() {
        assertThat(StringUtil.lastIndexOf("axaxa", 'x', 0, 2)).isEqualTo(1);
        assertThat(StringUtil.lastIndexOf("axaxa", 'x', 0, 3)).isEqualTo(1);
        assertThat(StringUtil.lastIndexOf("axaxa", 'x', 0, 5)).isEqualTo(3);
        assertThat(StringUtil.lastIndexOf("abcd", 'c', -42, 99)).isEqualTo(2);  // #IDEA-144968
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testLastIndexOfAny() {
        assertThat(StringUtil.lastIndexOfAny("axxa", "")).isEqualTo(-1);
        assertThat(StringUtil.lastIndexOfAny("axxa", "x")).isEqualTo(2);
        assertThat(StringUtil.lastIndexOfAny("axxa", "zx")).isEqualTo(2);
        assertThat(StringUtil.lastIndexOfAny("axxa", "z")).isEqualTo(-1);
    }

    @Test
    void testLastIndexOfIgnoreCaseChar() {
        assertThat(StringUtil.lastIndexOfIgnoreCase("FooBar", 'b', Integer.MAX_VALUE)).isEqualTo(3);
        assertThat(StringUtil.lastIndexOfIgnoreCase("FooBar", 'b', 6)).isEqualTo(3);
        assertThat(StringUtil.lastIndexOfIgnoreCase("FooBar", 'b', 3)).isEqualTo(3);
        assertThat(StringUtil.lastIndexOfIgnoreCase("FooBar", 'b', 2)).isEqualTo(-1);
        assertThat(StringUtil.lastIndexOfIgnoreCase("FooBar", 'b', Integer.MIN_VALUE)).isEqualTo(-1);
        assertThat(StringUtil.lastIndexOfIgnoreCase("FooBar", 'z', Integer.MAX_VALUE)).isEqualTo(-1);
        assertThat(StringUtil.lastIndexOfIgnoreCase("FooBar", 'z', 6)).isEqualTo(-1);
        assertThat(StringUtil.lastIndexOfIgnoreCase("FooBar", 'z', Integer.MIN_VALUE)).isEqualTo(-1);
    }

    @Test
    void testLength() {
        assertThat(StringUtil.length(null)).isEqualTo(0);
        assertThat(StringUtil.length("foo")).isEqualTo(3);
    }

    @Test
    void testLineColToOffset() {
        assertThat(StringUtil.lineColToOffset("\rfoo\r\nbar\nqux", 3, 2)).isEqualTo(12);
        assertThat(StringUtil.lineColToOffset("foo", 3, 2)).isEqualTo(-1);
    }

    @Test
    void testOffset() {
        assertThat(StringUtil.offsetToLineNumber("\rfoo\r\nbar\nqux", 12)).isEqualTo(3);
        assertThat(StringUtil.offsetToLineNumber("foo", 12)).isEqualTo(-1);
    }

    @Test
    void testNaturalCompare() {
        var numbers = Arrays.asList("1a000001", "000001a1", "001a0001", "0001A001", "00001a01", "01a00001");
        numbers.sort(NaturalComparator.INSTANCE);
        assertThat(numbers).containsExactly("1a000001", "01a00001", "001a0001", "0001A001", "00001a01", "000001a1");

        var test = Arrays.asList("test011", "test10", "test10a", "test010");
        test.sort(NaturalComparator.INSTANCE);
        assertThat(test).containsExactly("test10", "test10a", "test010", "test011");

        var strings = Arrays.asList(
            "Test99", "tes0", "test0", "testing", "test", "test99", "test011", "test1", "test 3", "test2",
            "test10a", "test10", "1.2.10.5", "1.2.9.1"
        );
        strings.sort(NaturalComparator.INSTANCE);
        assertThat(strings).containsExactly(
            "1.2.9.1", "1.2.10.5", "tes0", "test", "test0", "test1", "test2", "test 3", "test10", "test10a",
            "test011", "Test99", "test99", "testing"
        );

        var strings2 = Arrays.asList("t1", "t001", "T2", "T002", "T1", "t2");
        strings2.sort(NaturalComparator.INSTANCE);
        assertThat(strings2).containsExactly("T1", "t1", "t001", "T2", "t2", "T002");

        assertThat(StringUtil.naturalCompare("7403515080361171695", "07403515080361171694")).isPositive();
        assertThat(StringUtil.naturalCompare("_firstField", "myField1")).isNegative();

        var strings3 = Arrays.asList("C148A_InsomniaCure", "C148B_Escape", "C148C_TersePrincess", "C148D_BagOfMice", "C148E_Porcelain");
        strings3.sort(NaturalComparator.INSTANCE);
        assertThat(strings3).containsExactly(
            "C148A_InsomniaCure",
            "C148B_Escape",
            "C148C_TersePrincess",
            "C148D_BagOfMice",
            "C148E_Porcelain"
        );

        var l = Arrays.asList("a0002", "a0 2", "a001");
        l.sort(NaturalComparator.INSTANCE);
        assertThat(l).containsExactly("a0 2", "a001", "a0002");

        // Transitivity
        String s1 = "#";
        String s2 = "0b";
        String s3 = " 0b";
        assertThat(StringUtil.naturalCompare(s1, s2)).isLessThan(0);
        assertThat(StringUtil.naturalCompare(s2, s3)).isLessThan(0);
        assertThat(StringUtil.naturalCompare(s1, s3)).isLessThan(0);

        // Stability
        assertThat(StringUtil.naturalCompare("01a1", "1a01")).isNotSameAs(StringUtil.naturalCompare("1a01", "01a1"));
        assertThat(StringUtil.naturalCompare("#01A", "# 1A")).isNotSameAs(StringUtil.naturalCompare("# 1A", "#01A"));
        assertThat(StringUtil.naturalCompare("aA", "aa")).isNotSameAs(StringUtil.naturalCompare("aa", "aA"));
    }

    @Test
    void testNotNullize() {
        assertThat(StringUtil.notNullize(null)).isEqualTo("");
        assertThat(StringUtil.notNullize("foo")).isEqualTo("foo");
        assertThat(StringUtil.notNullize(null, "bar")).isEqualTo("bar");
        assertThat(StringUtil.notNullize("foo", "bar")).isEqualTo("foo");
    }

    @Test
    void testNotNullizeIfEmpty() {
        assertThat(StringUtil.notNullizeIfEmpty(null, "bar")).isEqualTo("bar");
        assertThat(StringUtil.notNullizeIfEmpty("", "bar")).isEqualTo("bar");
        assertThat(StringUtil.notNullizeIfEmpty("foo", "bar")).isEqualTo("foo");
    }

    @Test
    void testNullize() {
        assertThat(StringUtil.nullize("")).isNull();
        assertThat(StringUtil.nullize(" ")).isEqualTo(" ");
        assertThat(StringUtil.nullize("foo")).isEqualTo("foo");

        assertThat(StringUtil.nullize("", false)).isNull();
        assertThat(StringUtil.nullize(" ", false)).isEqualTo(" ");
        assertThat(StringUtil.nullize("foo", false)).isEqualTo("foo");
        assertThat(StringUtil.nullize("", true)).isNull();
        assertThat(StringUtil.nullize(" ", true)).isNull();
        assertThat(StringUtil.nullize("foo", true)).isEqualTo("foo");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testParseBoolean() {
        assertThat(StringUtil.parseBoolean(null, true)).isFalse();
        assertThat(StringUtil.parseBoolean("", true)).isFalse();
        assertThat(StringUtil.parseBoolean("false", true)).isFalse();
        assertThat(StringUtil.parseBoolean("true", false)).isTrue();
    }

    @Test
    void testParseDouble() {
        assertThat(StringUtil.parseDouble(null, -1)).isEqualTo(-1);
        assertThat(StringUtil.parseDouble("foo", -1)).isEqualTo(-1);
        assertThat(StringUtil.parseDouble("3.14", 0)).isEqualTo(3.14);
        assertThat(StringUtil.parseDouble("-1.23E45", 0)).isEqualTo(-1.23E45);
    }

    @Test
    void testParseInt() {
        assertThat(StringUtil.parseInt(null, -1)).isEqualTo(-1);
        assertThat(StringUtil.parseInt("foo", -1)).isEqualTo(-1);
        assertThat(StringUtil.parseInt("5", 0)).isEqualTo(5);
        assertThat(StringUtil.parseInt("-123456789", 0)).isEqualTo(-123456789);
    }

    @Test
    void testParseLong() {
        assertThat(StringUtil.parseLong(null, -1)).isEqualTo(-1);
        assertThat(StringUtil.parseLong("foo", -1)).isEqualTo(-1);
        assertThat(StringUtil.parseLong("5", 0)).isEqualTo(5);
        assertThat(StringUtil.parseLong("-1234567890123456789", 0)).isEqualTo(-1234567890123456789L);
    }

    @Test
    void testQuote() {
        StringBuilder sb = sb("foo");
        StringUtil.quote(sb);
        assertThat(sb).hasToString("\"foo\"");

        sb = sb("foo");
        StringUtil.quote(sb, '\'');
        assertThat(sb).hasToString("'foo'");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testRepeat() {
        assertThatThrownBy(() -> StringUtil.repeat("foo", -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Negative count: -1");
        assertThat(StringUtil.repeat("foo", 0)).isEqualTo("");
        assertThat(StringUtil.repeat("foo", 1)).isEqualTo("foo");
        assertThat(StringUtil.repeat("foo", 5)).isEqualTo("foofoofoofoofoo");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testRepeatSymbol() {
        assertThatThrownBy(() -> StringUtil.repeatSymbol('a', -1)).isInstanceOf(NegativeArraySizeException.class);
        assertThat(StringUtil.repeatSymbol('a', 0)).isEqualTo("");
        assertThat(StringUtil.repeatSymbol('a', 1)).isEqualTo("a");
        assertThat(StringUtil.repeatSymbol('a', 5)).isEqualTo("aaaaa");

        assertThatThrownBy(() -> StringUtil.repeatSymbol(sb(), 'a', -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Negative count: -1");

        StringBuilder sb = sb();
        StringUtil.repeatSymbol(sb, 'a', 0);
        assertThat(sb).hasToString("");

        sb = sb();
        StringUtil.repeatSymbol(sb, 'a', 1);
        assertThat(sb).hasToString("a");

        sb = sb();
        StringUtil.repeatSymbol(sb, 'a', 5);
        assertThat(sb).hasToString("aaaaa");
    }

    @Test
    void testReplace() {
        String pattern = "$PROJECT_FILE$".toLowerCase().toUpperCase();
        String replacement = "/tmp";
        assertThat(StringUtil.replace("$PROJECT_FILE$/filename", pattern, replacement)).isEqualTo("/tmp/filename");
        String tooShortFrom = "/filename";
        assertThat(StringUtil.replace(tooShortFrom, pattern, replacement)).isSameAs(tooShortFrom);
        String noPatternFrom = "/path/filename";
        assertThat(StringUtil.replace(noPatternFrom, pattern, replacement)).isSameAs(noPatternFrom);
        String onlyPatternFrom = "$PROJECT_FILE$";
        assertThat(StringUtil.replace(onlyPatternFrom, pattern, replacement)).isSameAs(replacement);
    }

    @Test
    void testReplaceArray() {
        assertThat(StringUtil.replace("&\"", new String[]{"&", "\""}, new String[]{"&amp;", "&quot;"}))
            .isEqualTo("&amp;&quot;");
        assertThat(StringUtil.replace("foobar", new String[]{"foobar", "foo", "bar"}, new String[]{"1", "2", "3"}))
            .isEqualTo("1");
        assertThat(StringUtil.replace("foobar", new String[]{"foo", "bar", "foobar"}, new String[]{"2", "3", "1"}))
            .isEqualTo("23");
    }

    @Test
    void testReplaceIgnoreCase() {
        String pattern = "$PROJECT_FILE$".toLowerCase().toUpperCase();
        String replacement = "/tmp";
        assertThat(StringUtil.replaceIgnoreCase("$project_file$/filename", pattern, replacement)).isEqualTo("/tmp/filename");
        String tooShortFrom = "/filename";
        assertThat(StringUtil.replaceIgnoreCase(tooShortFrom, pattern, replacement)).isSameAs(tooShortFrom);
        String noPatternFrom = "/path/filename";
        assertThat(StringUtil.replaceIgnoreCase(noPatternFrom, pattern, replacement)).isSameAs(noPatternFrom);
        String onlyPatternFrom = "$project_file$";
        assertThat(StringUtil.replaceIgnoreCase(onlyPatternFrom, pattern, replacement)).isSameAs(replacement);
    }

    @Test
    void testReplaceList() {
        assertThat(StringUtil.replace("&\"", List.of("&", "\""), List.of("&amp;", "&quot;")))
            .isEqualTo("&amp;&quot;");
        assertThat(StringUtil.replace("foobar", List.of("foobar", "foo", "bar"), List.of("1", "2", "3")))
            .isEqualTo("1");
        assertThat(StringUtil.replace("foobar", List.of("foo", "bar", "foobar"), List.of("2", "3", "1")))
            .isEqualTo("23");
    }

    @Test
    void testReplaceUnicodeEscapeSequences() {
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\uuu005a")).isEqualTo("Z");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\uuu005aZ")).isEqualTo("ZZ");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("Z\\uuu005aZ")).isEqualTo("ZZZ");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("Z\\\\uuu005aZ")).isEqualTo("Z\\\\uuu005aZ");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\uuu005\\a\\u1\\u22\\u333")).isEqualTo("\\uuu005\\a\\u1\\u22\\u333");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\u\\u0041\\u1\\u005a")).isEqualTo("\\uA\\u1Z");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\u004")).isEqualTo("\\u004");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\")).isEqualTo("\\");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\u")).isEqualTo("\\u");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\uu")).isEqualTo("\\uu");
        assertThat(StringUtil.replaceUnicodeEscapeSequences("\\uu1")).isEqualTo("\\uu1");
    }

    @Test
    void testSanitizeJavaIdentifier() {
        assertThat(StringUtil.sanitizeJavaIdentifier("0foo0/")).isEqualTo("_0foo0");
    }

    @Test
    void testShortenPathWithEllipsis() {
        assertThatThrownBy(() -> StringUtil.shortenPathWithEllipsis("foo/bar/baz/qux", 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("prefixLength = 0 for given textLength = 15, maxLength = 10 and suffixLength = 7");
        assertThat(StringUtil.shortenPathWithEllipsis("foo/bar/baz/qux", 14)).isEqualTo("fo...r/baz/qux");
        assertThat(StringUtil.shortenPathWithEllipsis("foo/bar/baz/qux", 14, false)).isEqualTo("fo...r/baz/qux");
        assertThat(StringUtil.shortenPathWithEllipsis("foo/bar/baz/qux", 14, true)).isEqualTo("foo/…r/baz/qux");
    }

    @Test
    void testShortenTextWithEllipsis() {
        assertThatThrownBy(() -> StringUtil.shortenTextWithEllipsis("foobar", 5, 2))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("prefixLength = 0 for given textLength = 6, maxLength = 5 and suffixLength = 2");
        assertThat(StringUtil.shortenTextWithEllipsis("foo", 5, 1)).isEqualTo("foo");
        assertThat(StringUtil.shortenTextWithEllipsis("foobar", 5, 1)).isEqualTo("f...r");
        assertThat(StringUtil.shortenTextWithEllipsis("foobar", 5, 1, false)).isEqualTo("f...r");
        assertThat(StringUtil.shortenTextWithEllipsis("foobar", 5, 2, true)).isEqualTo("fo…ar");
        assertThat(StringUtil.shortenTextWithEllipsis("foobar", 5, 2, "*")).isEqualTo("fo*ar");
    }

    @Test
    void testTrimMiddle() {
        assertThat(StringUtil.trimMiddle("foobar", 5)).isEqualTo("fo…ar");
        assertThat(StringUtil.trimMiddle("foobar", 5, true)).isEqualTo("fo…ar");
        assertThat(StringUtil.trimMiddle("foobar", 5, false)).isEqualTo("f...r");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testSkipWhitespaceBackward() {
        assertThat(StringUtil.skipWhitespaceBackward("x \t^", 3)).isEqualTo(1);
        assertThat(StringUtil.skipWhitespaceBackward("\n \t^", 3)).isEqualTo(1);
        assertThat(StringUtil.skipWhitespaceBackward("\r \t^", 3)).isEqualTo(1);
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testSkipWhitespaceForward() {
        assertThat(StringUtil.skipWhitespaceForward("^ \tx", 1)).isEqualTo(3);
        assertThat(StringUtil.skipWhitespaceForward("^ \t\n", 1)).isEqualTo(3);
        assertThat(StringUtil.skipWhitespaceForward("^ \t\r", 1)).isEqualTo(3);
    }

    @Test
    void testSplit() {
        String spaceSeparator = " ";
        assertThat(StringUtil.split("test", spaceSeparator, false, false)).containsExactly("test");
        CharSequenceSubSequence seq = new CharSequenceSubSequence("test");
        assertThat(StringUtil.split(seq, spaceSeparator, false, false)).containsExactly(seq);

        assertThat(StringUtil.split("", spaceSeparator, false, false)).containsExactly("");
        assertThat(StringUtil.split("", spaceSeparator, true, true)).isEmpty();

        assertThat(StringUtil.split(" ", spaceSeparator, false, false)).containsExactly(" ", "");
        assertThat(StringUtil.split(" ", spaceSeparator, true, false)).containsExactly("", "");
        assertThat(StringUtil.split(" ", spaceSeparator, true, true)).isEmpty();

        assertThat(StringUtil.split("a  b ", spaceSeparator, true, true)).containsExactly("a", "b");
        assertThat(StringUtil.split("a  b ", spaceSeparator, false, true)).containsExactly("a ", " ", "b ");
        assertThat(StringUtil.split("a  b ", spaceSeparator, false, false)).containsExactly("a ", " ", "b ", "");

        assertThat(StringUtil.split("a  b", spaceSeparator, true, true)).containsExactly("a", "b");
        assertThat(StringUtil.split("a  b", spaceSeparator, false, true)).containsExactly("a ", " ", "b");
        assertThat(StringUtil.split("a  b", spaceSeparator, false, false)).containsExactly("a ", " ", "b");

        assertThat(StringUtil.split("test", spaceSeparator, true, true)).containsExactly("test");
        assertThat(StringUtil.split("test", spaceSeparator, false, true)).containsExactly("test");
        assertThat(StringUtil.split("test", spaceSeparator, true, false)).containsExactly("test");
        assertThat(StringUtil.split("test", spaceSeparator, false, false)).containsExactly("test");

        assertThat(StringUtil.split("a  b ", spaceSeparator, true, true)).containsExactly("a", " b");
        assertThat(StringUtil.split("a \n\tb ", spaceSeparator, true, true)).containsExactly("a", "\n\tb");

        assertThat(StringUtil.split("a\u00A0b", spaceSeparator, true, true)).containsExactly("a\u00A0b");

        assertThat(StringUtil.split("a  \n\ta ", "a", true, true)).containsExactly("  \n\t", " ");
    }

    @Test
    void testSplitHonorQuotes() {
        // Merge separators
        assertThat(StringUtil.splitHonorQuotes("aaa bbb   ccc ", ' '))
            .containsExactly("aaa", "bbb", "ccc");
        // Support different quotes
        assertThat(StringUtil.splitHonorQuotes("'aaa' \"bbb\"", ' '))
            .containsExactly("'aaa'", "\"bbb\"");
        // Ignore separators inside quotes
        assertThat(StringUtil.splitHonorQuotes("'a aa' \"bb b\"", ' '))
            .containsExactly("'a aa'", "\"bb b\"");
        // Ignore other quotes inside quotes
        assertThat(StringUtil.splitHonorQuotes("'a\" aa' \"bb 'b\"", ' '))
            .containsExactly("'a\" aa'", "\"bb 'b\"");
        // Escape quotes
        assertThat(StringUtil.splitHonorQuotes("'a \\'aa' \"bb\\\" b\"", ' '))
            .containsExactly("'a \\'aa'", "\"bb\\\" b\"");
        // Unescape escaped quotes
        assertThat(StringUtil.splitHonorQuotes("'a aa\\\\' \"bb b\\\\\"", ' '))
            .containsExactly("'a aa\\\\'", "\"bb b\\\\\"");
        // Escape unescaped quotes
        assertThat(StringUtil.splitHonorQuotes("'a \\\\\\'aa' \"bb \\\\\\\"b\"", ' '))
            .containsExactly("'a \\\\\\'aa'", "\"bb \\\\\\\"b\"");
    }

    @Test
    void testStartsWith() {
        assertThat(StringUtil.startsWith("foo", "foobar")).isFalse();
        assertThat(StringUtil.startsWith("foobar", "foo")).isTrue();
        assertThat(StringUtil.startsWith("foobar", "baz")).isFalse();
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testStartsWith2() {
        assertTrue(StringUtil.startsWith("abcdefgh", 5, "fgh"));
        assertTrue(StringUtil.startsWith("abcdefgh", 2, "cde"));
        assertTrue(StringUtil.startsWith("abcdefgh", 0, "abc"));
        assertTrue(StringUtil.startsWith("abcdefgh", 0, "abcdefgh"));
        assertFalse(StringUtil.startsWith("abcdefgh", 5, "cde"));

        assertTrue(StringUtil.startsWith("abcdefgh", 0, ""));
        assertTrue(StringUtil.startsWith("abcdefgh", 4, ""));
        assertTrue(StringUtil.startsWith("abcdefgh", 7, ""));
        assertTrue(StringUtil.startsWith("abcdefgh", 8, ""));

        assertTrue(StringUtil.startsWith("", 0, ""));

        assertFalse(StringUtil.startsWith("ab", 0, "abcdefgh"));
        assertFalse(StringUtil.startsWith("ab", 1, "abcdefgh"));
        assertFalse(StringUtil.startsWith("ab", 2, "abcdefgh"));

        assertThatThrownBy(() -> StringUtil.startsWith("whatever", -1, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: -1, length: 8");
        assertThatThrownBy(() -> StringUtil.startsWith("whatever", 9, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: 9, length: 8");
        assertThatThrownBy(() -> StringUtil.startsWith("", -1, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: -1, length: 0");
        assertThatThrownBy(() -> StringUtil.startsWith("", 1, ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: 1, length: 0");
        assertThatThrownBy(() -> StringUtil.startsWith("wh", -1, "whatever"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: -1, length: 2");
        assertThatThrownBy(() -> StringUtil.startsWith("wh", 3, "whatever"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Index is out of bounds: 3, length: 2");
    }

    @Test
    void testStartsWithChar() {
        assertThat(StringUtil.startsWithChar(null, 'f')).isFalse();
        assertThat(StringUtil.startsWithChar("", 'f')).isFalse();
        assertThat(StringUtil.startsWithChar("foo", 'f')).isTrue();
    }

    @Test
    void testStartsWithConcatenation() {
        assertTrue(StringUtil.startsWithConcatenation("something.with.dot", "something", "."));
        assertTrue(StringUtil.startsWithConcatenation("something.with.dot", "", "something."));
        assertTrue(StringUtil.startsWithConcatenation("something.", "something", "."));
        assertTrue(StringUtil.startsWithConcatenation("something", "something", "", "", ""));
        assertFalse(StringUtil.startsWithConcatenation("something", "something", "", "", "."));
        assertFalse(StringUtil.startsWithConcatenation("some", "something", ""));
    }

    @Test
    void testStartsWithIgnoreCase() {
        assertThat(StringUtil.startsWithIgnoreCase("Foobar", "foo")).isTrue();
        assertThat(StringUtil.startsWithIgnoreCase("Foobar", "bar")).isFalse();
    }

    @Test
    void testStartsWithWhitespace() {
        assertThat(StringUtil.startsWithWhitespace("")).isFalse();
        assertThat(StringUtil.startsWithWhitespace("foo")).isFalse();
        assertThat(StringUtil.startsWithWhitespace(" ")).isTrue();
        assertThat(StringUtil.startsWithWhitespace("\t")).isTrue();
        assertThat(StringUtil.startsWithWhitespace("\n")).isTrue();
        assertThat(StringUtil.startsWithWhitespace("\r")).isTrue();
    }

    @Test
    void testStringHashCode() {
        assertThat(StringUtil.stringHashCode(sb("")))
            .isEqualTo(StringUtil.stringHashCode(""))
            .isEqualTo(StringUtil.stringHashCode(new char[0]))
            .isEqualTo("".hashCode());

        assertThat(StringUtil.stringHashCode(sb("foo")))
            .isEqualTo(StringUtil.stringHashCode("foo"))
            .isEqualTo(StringUtil.stringHashCode("foo".toCharArray()))
            .isEqualTo("foo".hashCode());
    }

    @Test
    void testStringHashCodeInsensitive() {
        assertThat(StringUtil.stringHashCodeInsensitive(""))
            .isEqualTo(StringUtil.stringHashCodeInsensitive(new char[0]));

        assertThat(StringUtil.stringHashCodeInsensitive("foo"))
            .isEqualTo(StringUtil.stringHashCodeInsensitive("foo".toCharArray()))
            .isEqualTo(StringUtil.stringHashCodeInsensitive("Foo"))
            .isEqualTo(StringUtil.stringHashCodeInsensitive("Foo".toCharArray()));
    }

    @Test
    void testStringHashCodeIgnoreWhitespaces() {
        assertThat(StringUtil.stringHashCodeIgnoreWhitespaces(""))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces(""))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces(new char[0]))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces(" \t\r\n"))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces(" \t\r\n".toCharArray()));

        assertThat(StringUtil.stringHashCodeIgnoreWhitespaces("x"))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces("x".toCharArray()))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces(" x"))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces("x "))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces(" x "))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces(" x ".toCharArray()))
            .isNotEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces(" \t\r\n"));

        assertThat(StringUtil.stringHashCodeIgnoreWhitespaces("foo"))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces(" f\to\ro\n"))
            .isEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces(" f\to\ro\n".toCharArray()));

        assertThat(StringUtil.stringHashCodeIgnoreWhitespaces("xyx"))
            .isNotEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces("xxx"))
            .isNotEqualTo(StringUtil.stringHashCodeIgnoreWhitespaces("xYx"));
    }

    @Test
    void testStripCharFilter() {
        assertThat(StringUtil.strip("\n   foo -bar ", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("foo-bar");
        assertThat(StringUtil.strip("foo- bar", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("foo-bar");
        assertThat(StringUtil.strip("foo-bar", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("foo-bar");
        assertThat(StringUtil.strip("\n   foo bar ", CharFilter.WHITESPACE_FILTER)).isEqualTo("\n     ");
        assertThat(StringUtil.strip("", CharFilter.WHITESPACE_FILTER)).isEqualTo("");
        assertThat(StringUtil.strip("\n   foo bar ", ch -> false)).isEqualTo("");
        assertThat(StringUtil.strip("\n   foo bar ", ch -> true)).isEqualTo("\n   foo bar ");
    }

    @Test
    void testStripHtml() {
        assertThat(StringUtil.stripHtml("<html>foo<br/>bar</html>", false)).isEqualTo("foobar");
        assertThat(StringUtil.stripHtml("<>foobar<>", false)).isEqualTo("foobar");
        assertThat(StringUtil.stripHtml("<div\nstyle=\"foo\">foobar</div>", true)).isEqualTo("foobar");
        assertThat(StringUtil.stripHtml("<html>foo<br/>bar<br></html>", true)).isEqualTo("foo\n\nbar\n\n");
    }

    @SuppressWarnings("SSBasedInspection")
    @Test
    void testStripQuotesAroundValue() {
        assertThat(StringUtil.stripQuotesAroundValue("")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("'")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("\"")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("''")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("\"\"")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("'\"")).isEqualTo("");
        assertThat(StringUtil.stripQuotesAroundValue("'foo'")).isEqualTo("foo");
        assertThat(StringUtil.stripQuotesAroundValue("'foo")).isEqualTo("foo");
        assertThat(StringUtil.stripQuotesAroundValue("foo'")).isEqualTo("foo");
        assertThat(StringUtil.stripQuotesAroundValue("'f'o'o'")).isEqualTo("f'o'o");
        assertThat(StringUtil.stripQuotesAroundValue("\"f\"o'o'")).isEqualTo("f\"o'o");
        assertThat(StringUtil.stripQuotesAroundValue("f\"o'o")).isEqualTo("f\"o'o");
        assertThat(StringUtil.stripQuotesAroundValue("\"\"'f\"o'o\"\"")).isEqualTo("\"'f\"o'o\"");
        assertThat(StringUtil.stripQuotesAroundValue("'''f\"o'o'''")).isEqualTo("''f\"o'o''");
        assertThat(StringUtil.stripQuotesAroundValue("foo' 'bar")).isEqualTo("foo' 'bar");
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testSubstringAfter() {
        assertThat(StringUtil.substringAfter("abc", "b")).isEqualTo("c");
        assertThat(StringUtil.substringAfter("ababbccc", "b")).isEqualTo("abbccc");
        assertThat(StringUtil.substringAfter("abc", "")).isEqualTo("abc");
        assertThat(StringUtil.substringAfter("abc", "1")).isNull();
        assertThat(StringUtil.substringAfter("", "1")).isNull();
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testSubstringAfterLast() {
        assertThat(StringUtil.substringAfterLast("abc", "b")).isEqualTo("c");
        assertThat(StringUtil.substringAfterLast("ababbccc", "b")).isEqualTo("ccc");
        assertThat(StringUtil.substringAfterLast("abc", "")).isEqualTo("");
        assertThat(StringUtil.substringAfterLast("abc", "1")).isNull();
        assertThat(StringUtil.substringAfterLast("", "1")).isNull();
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testSubstringBefore() {
        assertThat(StringUtil.substringBefore("abc", "b")).isEqualTo("a");
        assertThat(StringUtil.substringBefore("ababbccc", "b")).isEqualTo("a");
        assertThat(StringUtil.substringBefore("abc", "")).isEqualTo("");
        assertThat(StringUtil.substringBefore("abc", "1")).isNull();
        assertThat(StringUtil.substringBefore("", "1")).isNull();
    }

    @SuppressWarnings("SpellCheckingInspection")
    @Test
    void testSubstringBeforeLast() {
        assertThat(StringUtil.substringBeforeLast("abc", "b")).isEqualTo("a");
        assertThat(StringUtil.substringBeforeLast("ababbccc", "b")).isEqualTo("abab");
        assertThat(StringUtil.substringBeforeLast("abc", "")).isEqualTo("abc");
        assertThat(StringUtil.substringBeforeLast("abc", "1")).isEqualTo("abc");
        assertThat(StringUtil.substringBeforeLast("", "1")).isEqualTo("");
    }

    @Test
    void testSurround() {
        assertThat(StringUtil.surround(new String[0], "foo", "bar")).isEmpty();
        assertThat(StringUtil.surround(new String[]{"-", "+"}, "foo", "bar")).containsExactly("foo-bar", "foo+bar");
    }

    @Test
    void testTokenize() {
        assertThat(StringUtil.tokenize("foo/bar/baz", "/")).containsExactly("foo", "bar", "baz");
        assertThat(StringUtil.tokenize(new StringTokenizer("foo/bar/baz", "/"))).containsExactly("foo", "bar", "baz");
        assertThatThrownBy(() -> StringUtil.tokenize(new StringTokenizer("foo/bar/baz", "/")).iterator().remove())
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void testToLowerCaseChar() {
        assertThat(StringUtil.toLowerCase('/')).isEqualTo('/');
        assertThat(StringUtil.toLowerCase(':')).isEqualTo(':');
        assertThat(StringUtil.toLowerCase('a')).isEqualTo('a');
        assertThat(StringUtil.toLowerCase('A')).isEqualTo('a');
        assertThat(StringUtil.toLowerCase('k')).isEqualTo('k');
        assertThat(StringUtil.toLowerCase('K')).isEqualTo('k');

        for (char ch = 0; ch < 256; ch++) {
            assertThat(StringUtil.toLowerCase(ch)).isEqualTo(Character.toLowerCase(ch));
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testToLowerCaseString() {
        assertThat(StringUtil.toLowerCase(null)).isEqualTo(null);
        assertThat(StringUtil.toLowerCase("FOO")).isEqualTo("foo");
    }

    @Test
    void testToUpperCaseChar() {
        assertThat(StringUtil.toUpperCase('/')).isEqualTo('/');
        assertThat(StringUtil.toUpperCase(':')).isEqualTo(':');
        assertThat(StringUtil.toUpperCase('a')).isEqualTo('A');
        assertThat(StringUtil.toUpperCase('A')).isEqualTo('A');
        assertThat(StringUtil.toUpperCase('k')).isEqualTo('K');
        assertThat(StringUtil.toUpperCase('K')).isEqualTo('K');

        assertThat(StringUtil.toUpperCase(Character.toLowerCase('\u2567'))).isEqualTo('\u2567');

        for (char ch = 0; ch < 256; ch++) {
            assertThat(StringUtil.toUpperCase(ch)).isEqualTo(Character.toUpperCase(ch));
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testToUpperCaseString() {
        assertThat(StringUtil.toUpperCase(null)).isEqualTo(null);
        assertThat(StringUtil.toUpperCase("foo")).isEqualTo("FOO");
    }

    @Test
    void testToUpperCaseCharSequence() {
        assertThat(StringUtil.toUpperCase((CharSequence) "foo")).hasToString("FOO");
        String uppercased = "FOO123";
        assertThat(StringUtil.toUpperCase((CharSequence) uppercased)).isSameAs(uppercased);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testTrim() {
        assertThat(StringUtil.trim(null)).isNull();
        assertThat(StringUtil.trim("\n foo ")).isEqualTo("foo");
    }

    @Test
    void testTrimCharFilter() {
        assertThat(StringUtil.trim("\n   foo bar ", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("foo bar");
        assertThat(StringUtil.trim("foo bar", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("foo bar");
        assertThat(StringUtil.trim("foo bar\t", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("foo bar");
        assertThat(StringUtil.trim("\nfoo bar", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("foo bar");
        assertThat(StringUtil.trim("foo-bar", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("foo-bar");
        assertThat(StringUtil.trim("foo-bar ", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("foo-bar");
        assertThat(StringUtil.trim("\n   foo bar ", CharFilter.WHITESPACE_FILTER)).isEqualTo("\n   foo bar ");
        assertThat(StringUtil.trim("", CharFilter.WHITESPACE_FILTER)).isEqualTo("");
        assertThat(StringUtil.trim("\n   foo bar ", ch -> false)).isEqualTo("");
        assertThat(StringUtil.trim("\n   foo bar ", ch -> true)).isEqualTo("\n   foo bar ");
        assertThat(StringUtil.trim("\u00A0   foo bar ", CharFilter.NOT_WHITESPACE_FILTER)).isEqualTo("\u00A0   foo bar");
    }

    @Test
    void testTrimEnd() {
        StringBuilder foobar = new StringBuilder("foobar");
        assertThat(StringUtil.trimEnd(foobar, "bar")).isTrue();
        assertThat(foobar).hasToString("foo");

        StringBuilder qux = new StringBuilder("qux");
        assertThat(StringUtil.trimEnd(qux, "bar")).isFalse();
        assertThat(qux).hasToString("qux");
    }

    @Test
    void testTrimExtension() {
        assertThat(StringUtil.trimExtension("foo")).isEqualTo("foo");
        assertThat(StringUtil.trimExtension("foo.exe")).isEqualTo("foo");
        assertThat(StringUtil.trimExtension("foo.bar.exe")).isEqualTo("foo.bar");
    }

    @Test
    void testTrimLeadingChar() {
        assertTrimLeading("", "");
        assertTrimLeading("", " ");
        assertTrimLeading("", "    ");
        assertTrimLeading("a  ", "a  ");
        assertTrimLeading("a  ", "  a  ");
    }

    private static void assertTrimLeading(String expected, String string) {
        assertThat(StringUtil.trimLeading(string)).isEqualTo(expected);
        assertThat(StringUtil.trimLeading(string, ' ')).isEqualTo(expected);
        //assertThat(StringUtil.trimLeading(new StringBuilder(string), ' ').toString()).isEqualTo(expected);
    }

    @Test
    void testTrimLog() {
        assertThat(StringUtil.trimLog("Foo bar", 5)).isEqualTo("Foo bar");
        assertThat(StringUtil.trimLog("Foo bar", 6)).isEqualTo("F ...\n");
        assertThat(StringUtil.trimLog("Foo bar", 7)).isEqualTo("Foo bar");
    }

    @Test
    void testTrimTrailingChar() {
        assertTrimTrailing("", "");
        assertTrimTrailing(" ", "");
        assertTrimTrailing("    ", "");
        assertTrimTrailing("  a", "  a");
        assertTrimTrailing("  a  ", "  a");
    }

    private static void assertTrimTrailing(String string, String expected) {
        assertThat(StringUtil.trimTrailing(string))
            .isEqualTo(StringUtil.trimTrailing(string, ' '))
            .isEqualTo(StringUtil.trimTrailing(new StringBuilder(string), ' ').toString())
            .isEqualTo(expected);
    }

    @Test
    void testUnescapeBackSlashes() {
        assertThat(StringUtil.unescapeBackSlashes("\\\\\\\\server\\\\share\\\\extension.crx")).isEqualTo("\\\\server\\share\\extension.crx");
        assertThat(StringUtil.unescapeBackSlashes("\\")).isEqualTo("\\");
    }

    @Test
    void testUnescapeSlashes() {
        assertThat(StringUtil.unescapeSlashes("\\/")).isEqualTo("/");
        assertThat(StringUtil.unescapeSlashes("foo\\/bar\\foo\\/")).isEqualTo("foo/bar\\foo/");
        assertThat(StringUtil.unescapeSlashes("\\")).isEqualTo("\\");
    }

    @SuppressWarnings({"SpellCheckingInspection", "deprecation"})
    @Test
    void testUnescapeStringCharacters() {
        assertThat(StringUtil.unescapeStringCharacters("\\0\\12\\345\\456\\b\\f\\n\\r\\t\\u007F\\uFEff\\\"\\'\\\\foo"))
            .isEqualTo("\0\12\345%6\b\f\n\r\t\u007F\uFEFF\"'\\foo");
        assertThat(StringUtil.unescapeStringCharacters("\\uXXXX")).isEqualTo("\\uXXXX");
        assertThat(StringUtil.unescapeStringCharacters("\\uXXX")).isEqualTo("\\uXXX");
        assertThat(StringUtil.unescapeStringCharacters("\\z\\")).isEqualTo("z\\");

        StringBuilder sb = sb();
        StringUtil.unescapeStringCharacters(6, "\\\\\\\"\\n", sb);
        assertThat(sb).hasToString("\\\"\n");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testUnescapeXml() {
        assertThat(StringUtil.unescapeXml(null)).isNull();
        assertThat(StringUtil.unescapeXml("&lt;&amp;&#39;&quot;&gt;")).isEqualTo("<&'\">");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testUnquote() {
        assertThat(StringUtil.unquoteString("")).isEqualTo("");
        assertThat(StringUtil.unquoteString("\"")).isEqualTo("\"");
        assertThat(StringUtil.unquoteString("\"\"")).isEqualTo("");
        assertThat(StringUtil.unquoteString("\"\"\"")).isEqualTo("\"");
        assertThat(StringUtil.unquoteString("\"foo\"")).isEqualTo("foo");
        assertThat(StringUtil.unquoteString("\"foo")).isEqualTo("\"foo");
        assertThat(StringUtil.unquoteString("foo\"")).isEqualTo("foo\"");
        assertThat(StringUtil.unquoteString("")).isEqualTo("");
        assertThat(StringUtil.unquoteString("'")).isEqualTo("'");
        assertThat(StringUtil.unquoteString("''")).isEqualTo("");
        assertThat(StringUtil.unquoteString("'''")).isEqualTo("'");
        assertThat(StringUtil.unquoteString("'foo'")).isEqualTo("foo");
        assertThat(StringUtil.unquoteString("'foo")).isEqualTo("'foo");
        assertThat(StringUtil.unquoteString("foo'")).isEqualTo("foo'");

        assertThat(StringUtil.unquoteString("'\"")).isEqualTo("'\"");
        assertThat(StringUtil.unquoteString("\"'")).isEqualTo("\"'");
        assertThat(StringUtil.unquoteString("\"foo'")).isEqualTo("\"foo'");
    }

    @SuppressWarnings("deprecation")
    @Test
    void testUnquoteWithQuotationChar() {
        assertThat(StringUtil.unquoteString("", '|')).isEqualTo("");
        assertThat(StringUtil.unquoteString("|", '|')).isEqualTo("|");
        assertThat(StringUtil.unquoteString("||", '|')).isEqualTo("");
        assertThat(StringUtil.unquoteString("|||", '|')).isEqualTo("|");
        assertThat(StringUtil.unquoteString("|foo|", '|')).isEqualTo("foo");
        assertThat(StringUtil.unquoteString("|foo", '|')).isEqualTo("|foo");
        assertThat(StringUtil.unquoteString("foo|", '|')).isEqualTo("foo|");
    }

//    @Test
//    void doTestTrimCharSequence() {
//        assertThat(StringUtil.trim((CharSequence) "").toString()).isEqualTo("");
//        assertThat(StringUtil.trim((CharSequence) " ").toString()).isEqualTo("");
//        assertThat(StringUtil.trim((CharSequence) " \n\t\r").toString()).isEqualTo("");
//        assertThat(StringUtil.trim((CharSequence) "a").toString()).isEqualTo("a");
//        assertThat(StringUtil.trim((CharSequence) " a").toString()).isEqualTo("a");
//        assertThat(StringUtil.trim((CharSequence) "bc ").toString()).isEqualTo("bc");
//        assertThat(StringUtil.trim((CharSequence) " b a c   ").toString()).isEqualTo("b a c");
//    }

//    @Test
//    @SuppressWarnings("SpellCheckingInspection")
//    void testUnPluralize() {
//        // synthetic
//        assertThat(StringUtil.unpluralize("pluralses")).isEqualTo("plurals");
//        assertThat(StringUtil.unpluralize("Inheritses")).isEqualTo("Inherits");
//        assertThat(StringUtil.unpluralize("ss")).isEqualTo("s");
//        assertThat(StringUtil.unpluralize("Is")).isEqualTo("I");
//        assertThat(StringUtil.unpluralize("s")).isNull();
//        assertThat(StringUtil.unpluralize("zs")).isEqualTo("z");
//        // normal
//        assertThat(StringUtil.unpluralize("cases")).isEqualTo("case");
//        assertThat(StringUtil.unpluralize("Indices")).isEqualTo("Index");
//        assertThat(StringUtil.unpluralize("fixes")).isEqualTo("fix");
//        assertThat(StringUtil.unpluralize("men")).isEqualTo("man");
//        assertThat(StringUtil.unpluralize("leaves")).isEqualTo("leaf");
//        assertThat(StringUtil.unpluralize("cookies")).isEqualTo("cookie");
//        assertThat(StringUtil.unpluralize("searches")).isEqualTo("search");
//        assertThat(StringUtil.unpluralize("process")).isEqualTo("process");
//        assertThat(StringUtil.unpluralize("PROPERTIES")).isEqualTo("PROPERTY");
//        assertThat(StringUtil.unpluralize("THESE")).isEqualTo("THIS");
//        assertThat(StringUtil.unpluralize("databases")).isEqualTo("database");
//        assertThat(StringUtil.unpluralize("bases")).isEqualTo("basis");
//    }

//    @Test
//    void testPluralize() {
//        assertThat(StringUtil.pluralize("value")).isEqualTo("values");
//        assertThat(StringUtil.pluralize("values")).isEqualTo("values");
//        assertThat(StringUtil.pluralize("index")).isEqualTo("indices");
//        assertThat(StringUtil.pluralize("matrix")).isEqualTo("matrices");
//        assertThat(StringUtil.pluralize("fix")).isEqualTo("fixes");
//        assertThat(StringUtil.pluralize("man")).isEqualTo("men");
//        assertThat(StringUtil.pluralize("medium")).isEqualTo("media");
//        assertThat(StringUtil.pluralize("stash")).isEqualTo("stashes");
//        assertThat(StringUtil.pluralize("child")).isEqualTo("children");
//        assertThat(StringUtil.pluralize("leaf")).isEqualTo("leaves");
//        assertThat(StringUtil.pluralize("This")).isEqualTo("These");
//        assertThat(StringUtil.pluralize("cookie")).isEqualTo("cookies");
//        assertThat(StringUtil.pluralize("VaLuE")).isEqualTo("VaLuES");
//        assertThat(StringUtil.pluralize("PLAN")).isEqualTo("PLANS");
//        assertThat(StringUtil.pluralize("stackTraceLineEx")).isEqualTo("stackTraceLineExes");
//        assertThat(StringUtil.pluralize("schema")).isEqualTo("schemas"); // anglicized version
//        assertThat(StringUtil.pluralize("PROPERTY")).isEqualTo("PROPERTIES");
//        assertThat(StringUtil.pluralize("THIS")).isEqualTo("THESE");
//        assertThat(StringUtil.pluralize("database")).isEqualTo("databases");
//        assertThat(StringUtil.pluralize("base")).isEqualTo("bases");
//        assertThat(StringUtil.pluralize("basis")).isEqualTo("bases");
//    }

//    @Test
//    void testNaturalCompareTransitivityProperty() {
//        PropertyChecker.forAll(Surrogate.Generator.listsOf(Surrogate.Generator.stringsOf("ab01()_# ")), l -> {
//            List<String> sorted = ContainerUtil.sorted(l, StringUtil::naturalCompare);
//            for (int i = 0; i < sorted.size(); i++) {
//                for (int j = i + 1; j < sorted.size(); j++) {
//                    if (StringUtil.naturalCompare(sorted.get(i), sorted.get(j)) > 0) return false;
//                    if (StringUtil.naturalCompare(sorted.get(j), sorted.get(i)) < 0) return false;
//                }
//            }
//            return true;
//        });
//    }

//    @Test
//    void testFormatLinks() {
//        assertEquals("<a href=\"http://a-b+c\">http://a-b+c</a>", StringUtil.formatLinks("http://a-b+c"));
//    }

//    @Test
//    void testTitleCase() {
//        assertThat(StringUtil.wordsToBeginFromUpperCase("Couldn't connect to debugger")).isEqualTo("Couldn't Connect to Debugger");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("Let's make abbreviations like I18n, SQL and CSS"))
//            .isEqualTo("Let's Make Abbreviations Like I18n, SQL and CSS");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("1s_t _how a&re mn_emonics &handled, or aren't they"))
//            .isEqualTo("1s_t _How A&re Mn_emonics &Handled, or Aren't They");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("a good steak should not be this hard to come by"))
//            .isEqualTo("A Good Steak Should Not Be This Hard to Come By");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("twenty-one quick-fixes")).isEqualTo("Twenty-One Quick-Fixes");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("it's not a question of if, but when")).isEqualTo(
//            "It's Not a Question of If, but When");
//        assertThat(StringUtil.wordsToBeginFromUpperCase("scroll to the end. a good steak should not be this hard to come by."))
//            .isEqualTo("Scroll to the End. A Good Steak Should Not Be This Hard to Come By.");
//    }

//    @Test
//    void testSentenceCapitalization() {
//        assertThat(StringUtil.wordsToBeginFromLowerCase("Couldn't Connect To Debugger")).isEqualTo("couldn't connect to debugger");
//        assertThat(StringUtil.wordsToBeginFromLowerCase("Let's Make Abbreviations Like I18n, SQL and CSS S SQ Sq"))
//            .isEqualTo("let's make abbreviations like I18n, SQL and CSS s SQ sq");
//    }

//    @Test
//    void testCapitalizeWords() {
//        assertThat(StringUtil.capitalizeWords("AspectJ (syntax highlighting only)", true)).isEqualTo("AspectJ (Syntax Highlighting Only)");
//    }

//    @Test
//    void testSplitByLineKeepingSeparators() {
//        assertThat(StringUtil.splitByLinesKeepSeparators("")).containsExactly("");
//        assertThat(StringUtil.splitByLinesKeepSeparators("aa")).containsExactly("aa");
//        assertThat(StringUtil.splitByLinesKeepSeparators("\n\naa\n\nbb\ncc\n\n"))
//            .containsExactly("\n", "\n", "aa\n", "\n", "bb\n", "cc\n", "\n");
//
//        assertThat(StringUtil.splitByLinesKeepSeparators("\r\r\n\r")).containsExactly("\r", "\r\n", "\r");
//        assertThat(StringUtil.splitByLinesKeepSeparators("\r\n\r\r\n")).containsExactly("\r\n", "\r", "\r\n");
//
//        assertThat(StringUtil.splitByLinesKeepSeparators("\n\r\n\n\r\n\r\raa\rbb\r\ncc\n\rdd\n\n\r\n\r"))
//            .containsExactly("\n", "\r\n", "\n", "\r\n", "\r", "\r", "aa\r", "bb\r\n", "cc\n", "\r", "dd\n", "\n", "\r\n", "\r");
//    }

//    @Test
//    void testDetectSeparators() {
//        assertThat(StringUtil.detectSeparators("")).isNull();
//        assertThat(StringUtil.detectSeparators("asd")).isNull();
//        assertThat(StringUtil.detectSeparators("asd\t")).isNull();
//
//        assertEquals(LineSeparator.Unix, StringUtil.detectSeparators("asd\n"));
//        assertEquals(LineSeparator.Unix, StringUtil.detectSeparators("asd\nads\r"));
//        assertEquals(LineSeparator.Unix, StringUtil.detectSeparators("asd\nads\n"));
//
//        assertEquals(LineSeparator.Macintosh, StringUtil.detectSeparators("asd\r"));
//        assertEquals(LineSeparator.Macintosh, StringUtil.detectSeparators("asd\rads\r"));
//        assertEquals(LineSeparator.Macintosh, StringUtil.detectSeparators("asd\rads\n"));
//
//        assertEquals(LineSeparator.Windows, StringUtil.detectSeparators("asd\r\n"));
//        assertEquals(LineSeparator.Windows, StringUtil.detectSeparators("asd\r\nads\r"));
//        assertEquals(LineSeparator.Windows, StringUtil.detectSeparators("asd\r\nads\n"));
//    }

//    @Test
//    void testFindStartingLineSeparator() {
//        assertNull(StringUtil.getLineSeparatorAt("", -1));
//        assertNull(StringUtil.getLineSeparatorAt("", 0));
//        assertNull(StringUtil.getLineSeparatorAt("", 1));
//        assertNull(StringUtil.getLineSeparatorAt("\nHello", -1));
//        assertNull(StringUtil.getLineSeparatorAt("\nHello", 1));
//        assertNull(StringUtil.getLineSeparatorAt("\nH\rel\nlo", 6));
//
//        assertEquals(LineSeparator.Unix, StringUtil.getLineSeparatorAt("\nHello", 0));
//        assertEquals(LineSeparator.Unix, StringUtil.getLineSeparatorAt("\nH\rel\nlo", 5));
//        assertEquals(LineSeparator.Unix, StringUtil.getLineSeparatorAt("Hello\n", 5));
//
//        assertEquals(LineSeparator.Macintosh, StringUtil.getLineSeparatorAt("\rH\r\nelp", 0));
//        assertEquals(LineSeparator.Macintosh, StringUtil.getLineSeparatorAt("Hello\r", 5));
//        assertEquals(LineSeparator.Macintosh, StringUtil.getLineSeparatorAt("Hello\b\r", 6));
//
//        assertEquals(LineSeparator.Windows, StringUtil.getLineSeparatorAt("\rH\r\nelp", 2));
//        assertEquals(LineSeparator.Windows, StringUtil.getLineSeparatorAt("\r\nH\r\nelp", 0));
//        assertEquals(LineSeparator.Windows, StringUtil.getLineSeparatorAt("\r\nH\r\nelp\r\n", 8));
//    }

//    @Test
//    void testFormatFileSizeFixedPrecision() {
//        assertThat(StringUtil.formatFileSize(10, " ", -1, true)).isEqualTo("10.00 B");
//        assertThat(StringUtil.formatFileSize(100, " ", -1, true)).isEqualTo("100.00 B");
//        assertThat(StringUtil.formatFileSize(1_000, " ", -1, true)).isEqualTo("1.00 kB");
//        assertThat(StringUtil.formatFileSize(10_000, " ", -1, true)).isEqualTo("10.00 kB");
//        assertThat(StringUtil.formatFileSize(100_000, " ", -1, true)).isEqualTo("100.00 kB");
//        assertThat(StringUtil.formatFileSize(1_000_000, " ", -1, true)).isEqualTo("1.00 MB");
//        assertThat(StringUtil.formatFileSize(10_000_000, " ", -1, true)).isEqualTo("10.00 MB");
//        assertThat(StringUtil.formatFileSize(100_000_000, " ", -1, true)).isEqualTo("100.00 MB");
//        assertThat(StringUtil.formatFileSize(1_000_000_000, " ", -1, true)).isEqualTo("1.00 GB");
//    }

//    @Test
//    void testGetWordIndicesIn() {
//        assertThat(StringUtil.getWordIndicesIn("first second")).containsExactly(new TextRange(0, 5), new TextRange(6, 12));
//        assertThat(StringUtil.getWordIndicesIn(" first second")).containsExactly(new TextRange(1, 6), new TextRange(7, 13));
//        assertThat(StringUtil.getWordIndicesIn(" first second    ")).containsExactly(new TextRange(1, 6), new TextRange(7, 13));
//        assertThat(StringUtil.getWordIndicesIn("first:second")).containsExactly(new TextRange(0, 5), new TextRange(6, 12));
//        assertThat(StringUtil.getWordIndicesIn("first-second")).containsExactly(new TextRange(0, 5), new TextRange(6, 12));
//        assertThat(StringUtil.getWordIndicesIn("first-second", Set.of(' ', '_', '.'))).containsExactly(new TextRange(0, 12));
//        assertThat(StringUtil.getWordIndicesIn("first-second", Set.of('-'))).containsExactly(new TextRange(0, 5), new TextRange(6, 12));
//    }

//    @Test
//    @SuppressWarnings({"SpellCheckingInspection", "NonAsciiCharacters"})
//    void testIsLatinAlphanumeric() {
//        assertTrue(StringUtil.isLatinAlphanumeric("1234567890"));
//        assertTrue(StringUtil.isLatinAlphanumeric("123abc593"));
//        assertTrue(StringUtil.isLatinAlphanumeric("gwengioewn"));
//        assertTrue(StringUtil.isLatinAlphanumeric("FiwnFWinfs"));
//        assertTrue(StringUtil.isLatinAlphanumeric("b"));
//        assertTrue(StringUtil.isLatinAlphanumeric("1"));
//
//        assertFalse(StringUtil.isLatinAlphanumeric("йцукен"));
//        assertFalse(StringUtil.isLatinAlphanumeric("ЙцуTYuio"));
//        assertFalse(StringUtil.isLatinAlphanumeric("йцу626кен"));
//        assertFalse(StringUtil.isLatinAlphanumeric("12 12"));
//        assertFalse(StringUtil.isLatinAlphanumeric("."));
//        assertFalse(StringUtil.isLatinAlphanumeric("_"));
//        assertFalse(StringUtil.isLatinAlphanumeric("-"));
//        assertFalse(StringUtil.isLatinAlphanumeric("fhu384 "));
//        assertFalse(StringUtil.isLatinAlphanumeric(""));
//        assertFalse(StringUtil.isLatinAlphanumeric(null));
//        assertFalse(StringUtil.isLatinAlphanumeric("'"));
//    }

//    @Test
//    void testIsShortNameOf() {
//        assertTrue(StringUtil.isShortNameOf("a.b.c", "c"));
//        assertTrue(StringUtil.isShortNameOf("foo", "foo"));
//        assertFalse(StringUtil.isShortNameOf("foo", ""));
//        assertFalse(StringUtil.isShortNameOf("", "foo"));
//        assertFalse(StringUtil.isShortNameOf("a.b.c", "d"));
//        assertFalse(StringUtil.isShortNameOf("x.y.zzz", "zz"));
//        assertFalse(StringUtil.isShortNameOf("x", "a.b.x"));
//    }

//    @Test
//    void offsetToLineNumberCol() {
//        assertEquals(LineColumn.of(0, 0), StringUtil.offsetToLineColumn("abc\nabc", 0));
//        assertEquals(LineColumn.of(0, 1), StringUtil.offsetToLineColumn("abc\nabc", 1));
//        assertEquals(LineColumn.of(0, 2), StringUtil.offsetToLineColumn("abc\nabc", 2));
//        assertEquals(LineColumn.of(0, 3), StringUtil.offsetToLineColumn("abc\nabc", 3));
//        assertEquals(LineColumn.of(1, 0), StringUtil.offsetToLineColumn("abc\nabc", 4));
//        assertEquals(LineColumn.of(1, 1), StringUtil.offsetToLineColumn("abc\nabc", 5));
//        assertEquals(LineColumn.of(1, 3), StringUtil.offsetToLineColumn("abc\nabc", 7));
//        assertNull(StringUtil.offsetToLineColumn("abc\nabc", 8));
//        assertEquals(LineColumn.of(0, 3), StringUtil.offsetToLineColumn("abc\r\nabc", 3));
//        assertEquals(LineColumn.of(1, 0), StringUtil.offsetToLineColumn("abc\r\nabc", 5));
//        assertEquals(LineColumn.of(2, 1), StringUtil.offsetToLineColumn("abc\n\nabc", 6));
//        assertEquals(LineColumn.of(1, 1), StringUtil.offsetToLineColumn("abc\r\nabc", 6));
//    }

//    @Test
//    void testEnglishOrdinals() {
//        assertEquals("100th", OrdinalFormat.formatEnglish(100));
//        assertEquals("101st", OrdinalFormat.formatEnglish(101));
//        assertEquals("111th", OrdinalFormat.formatEnglish(111));
//        assertEquals("122nd", OrdinalFormat.formatEnglish(122));
//
//        assertEquals("-3rd", OrdinalFormat.formatEnglish(-3));
//        assertEquals("-9223372036854775808th", OrdinalFormat.formatEnglish(Long.MIN_VALUE));
//    }

//    @Test
//    void testCollapseWhiteSpace() {
//        assertEquals("one two three four five", StringUtil.collapseWhiteSpace("\t one\ttwo     three\nfour five   "));
//        assertEquals("one two three four five", StringUtil.collapseWhiteSpace(" one \ttwo  \t  three\n\tfour five "));
//    }

//    @Test
//    void testRemoveEllipsisSuffix() {
//        assertEquals("a", removeEllipsisSuffix("a..."));
//        assertEquals("a", removeEllipsisSuffix("a"));
//        assertEquals("a", removeEllipsisSuffix("a" + ELLIPSIS));
//        assertEquals("a...", removeEllipsisSuffix("a..." + ELLIPSIS));
//    }

//    @SuppressWarnings("UnnecessaryUnicodeEscape")
//    @Test
//    void testCharSequenceSliceIsJavaIdentifier() {
//        assertFalse(StringUtil.isJavaIdentifier("", 0, 0));
//        assertTrue(StringUtil.isJavaIdentifier("x", 0, 1));
//        assertFalse(StringUtil.isJavaIdentifier("0", 0, 1));
//        assertFalse(StringUtil.isJavaIdentifier("0x", 0, 2));
//        assertTrue(StringUtil.isJavaIdentifier("foo$bar", 0, 7));
//        assertTrue(StringUtil.isJavaIdentifier("x0", 0, 2));
//        assertTrue(StringUtil.isJavaIdentifier("\uD835\uDEFCA", 0, 3));
//        assertTrue(StringUtil.isJavaIdentifier("A\uD835\uDEFC", 0, 3));
//        assertTrue(StringUtil.isJavaIdentifier("\u03B1A", 0, 2));
//        assertTrue(StringUtil.isJavaIdentifier("###\u03B1A", 3, 5));
//        assertTrue(StringUtil.isJavaIdentifier("\u03B1A###", 0, 2));
//        assertTrue(StringUtil.isJavaIdentifier("###\u03B1A###", 3, 5));
//    }

//    @Test
//    void testSplitCharFilter() {
//        CharFilter spaceSeparator = CharFilter.WHITESPACE_FILTER;
//        //noinspection rawtypes
//        List split1 = StringUtil.split("test", spaceSeparator, false, false);
//        List split2 = StringUtil.split(new CharSequenceSubSequence("test"), spaceSeparator, false, false);
//        assertTrue(ContainerUtil.getOnlyItem(split1) instanceof String);
//        assertTrue(ContainerUtil.getOnlyItem(split2) instanceof CharSequenceSubSequence);
//
//        assertEquals(Arrays.asList(""), StringUtil.split("", spaceSeparator, false, false));
//        assertEquals(Arrays.asList(), StringUtil.split("", spaceSeparator, true, true));
//
//        assertEquals(Arrays.asList(" ", ""), StringUtil.split(" ", spaceSeparator, false, false));
//        assertEquals(Arrays.asList("", ""), StringUtil.split(" ", spaceSeparator, true, false));
//        assertEquals(Arrays.asList(), StringUtil.split(" ", spaceSeparator, true, true));
//
//        assertEquals(Arrays.asList("a", "b"), StringUtil.split("a  b ", spaceSeparator, true, true));
//        assertEquals(Arrays.asList("a ", " ", "b "), StringUtil.split("a  b ", spaceSeparator, false, true));
//        assertEquals(Arrays.asList("a ", " ", "b ", ""), StringUtil.split("a  b ", spaceSeparator, false, false));
//
//        assertEquals(Arrays.asList("a", "b"), StringUtil.split("a  b", spaceSeparator, true, true));
//        assertEquals(Arrays.asList("a ", " ", "b"), StringUtil.split("a  b", spaceSeparator, false, true));
//        assertEquals(Arrays.asList("a ", " ", "b"), StringUtil.split("a  b", spaceSeparator, false, false));
//
//        assertEquals(Arrays.asList("test"), StringUtil.split("test", spaceSeparator, true, true));
//        assertEquals(Arrays.asList("test"), StringUtil.split("test", spaceSeparator, false, true));
//        assertEquals(Arrays.asList("test"), StringUtil.split("test", spaceSeparator, true, false));
//        assertEquals(Arrays.asList("test"), StringUtil.split("test", spaceSeparator, false, false));
//
//        assertEquals(Arrays.asList("a", "b"), StringUtil.split("a  b ", spaceSeparator, true, true));
//        assertEquals(Arrays.asList("a", "b"), StringUtil.split("a \n\tb ", spaceSeparator, true, true));
//
//        assertEquals(Arrays.asList("a\u00A0b"), StringUtil.split("a\u00A0b", spaceSeparator, true, true));
//
//        assertEquals(Arrays.asList("  \n\t", " "), StringUtil.split("a  \n\ta ", CharFilter.NOT_WHITESPACE_FILTER, true, true));
//    }

//    @Test
//    @SuppressWarnings({"OctalInteger", "UnnecessaryUnicodeEscape"}) // need to test octal numbers and escapes
//    void testUnescapeAnsiStringCharacters() {
//        assertEquals("'", StringUtil.unescapeAnsiStringCharacters("\\'"));
//        assertEquals("\"", StringUtil.unescapeAnsiStringCharacters("\\\""));
//        assertEquals("?", StringUtil.unescapeAnsiStringCharacters("\\?"));
//        assertEquals("\\", StringUtil.unescapeAnsiStringCharacters("\\\\"));
//        assertEquals("" + (char)0x07, StringUtil.unescapeAnsiStringCharacters("\\a"));
//        assertEquals("" + (char)0x08, StringUtil.unescapeAnsiStringCharacters("\\b"));
//        assertEquals("" + (char)0x0c, StringUtil.unescapeAnsiStringCharacters("\\f"));
//        assertEquals("\n", StringUtil.unescapeAnsiStringCharacters("\\n"));
//        assertEquals("\r", StringUtil.unescapeAnsiStringCharacters("\\r"));
//        assertEquals("\t", StringUtil.unescapeAnsiStringCharacters("\\t"));
//        assertEquals("" + (char)0x0b, StringUtil.unescapeAnsiStringCharacters("\\v"));
//
//        // octal
//        assertEquals("" + (char)00, StringUtil.unescapeAnsiStringCharacters("\\0"));
//        assertEquals("" + (char)01, StringUtil.unescapeAnsiStringCharacters("\\1"));
//        assertEquals("" + (char)012, StringUtil.unescapeAnsiStringCharacters("\\12"));
//        assertEquals("" + (char)0123, StringUtil.unescapeAnsiStringCharacters("\\123"));
//
//        // hex
//        assertEquals("" + (char)0x0, StringUtil.unescapeAnsiStringCharacters("\\x0"));
//        assertEquals("" + (char)0xf, StringUtil.unescapeAnsiStringCharacters("\\xf"));
//        assertEquals("" + (char)0xff, StringUtil.unescapeAnsiStringCharacters("\\xff"));
//        assertEquals("" + (char)0xfff, StringUtil.unescapeAnsiStringCharacters("\\xfff"));
//        assertEquals("" + (char)0xffff, StringUtil.unescapeAnsiStringCharacters("\\xffff"));
//        assertEquals("" + (char)0xf, StringUtil.unescapeAnsiStringCharacters("\\x0000000000000000f"));
//        assertEquals("\\x110000", StringUtil.unescapeAnsiStringCharacters("\\x110000")); // invalid unicode codepoint
//
//        // 4 digit codepoint
//        assertEquals("\u1234", StringUtil.unescapeAnsiStringCharacters("\\u1234"));
//
//        // 8 digit codepoint
//        assertEquals("\u0061", StringUtil.unescapeAnsiStringCharacters("\\U00000061"));
//        assertEquals("\\U00110000", StringUtil.unescapeAnsiStringCharacters("\\U00110000")); // invalid unicode codepoint
//    }

    private static StringBuilder sb() {
        return new StringBuilder();
    }

    private static StringBuilder sb(String text) {
        return new StringBuilder(text);
    }
}
