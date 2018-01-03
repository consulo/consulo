/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.text;

import com.intellij.openapi.util.text.NaturalComparator;
import com.intellij.openapi.util.text.StringUtil;
import org.junit.Assert;
import org.junit.Test;

import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 *         Date: Dec 22, 2006
 */
public class StringUtilTest extends Assert {
  @Test
  public void testToUpperCase() {
    assertEquals('/', StringUtil.toUpperCase('/'));
    assertEquals(':', StringUtil.toUpperCase(':'));
    assertEquals('A', StringUtil.toUpperCase('a'));
    assertEquals('A', StringUtil.toUpperCase('A'));
    assertEquals('K', StringUtil.toUpperCase('k'));
    assertEquals('K', StringUtil.toUpperCase('K'));

    assertEquals('\u2567', StringUtil.toUpperCase(Character.toLowerCase('\u2567')));
  }

  @Test
  public void testToLowerCase() {
    assertEquals('/', StringUtil.toLowerCase('/'));
    assertEquals(':', StringUtil.toLowerCase(':'));
    assertEquals('a', StringUtil.toLowerCase('a'));
    assertEquals('a', StringUtil.toLowerCase('A'));
    assertEquals('k', StringUtil.toLowerCase('k'));
    assertEquals('k', StringUtil.toLowerCase('K'));

    assertEquals('\u2567', StringUtil.toUpperCase(Character.toLowerCase('\u2567')));
  }

  @Test
  public void testIsEmptyOrSpaces() throws Exception {
    assertTrue(StringUtil.isEmptyOrSpaces(null));
    assertTrue(StringUtil.isEmptyOrSpaces(""));
    assertTrue(StringUtil.isEmptyOrSpaces("                   "));

    assertFalse(StringUtil.isEmptyOrSpaces("1"));
    assertFalse(StringUtil.isEmptyOrSpaces("         12345          "));
    assertFalse(StringUtil.isEmptyOrSpaces("test"));
  }

  @Test
  public void testSplitWithQuotes() {
    final List<String> strings = StringUtil.splitHonorQuotes("aaa bbb   ccc \"ddd\" \"e\\\"e\\\"e\"  ", ' ');
    assertEquals(5, strings.size());
    assertEquals("aaa", strings.get(0));
    assertEquals("bbb", strings.get(1));
    assertEquals("ccc", strings.get(2));
    assertEquals("\"ddd\"", strings.get(3));
    assertEquals("\"e\\\"e\\\"e\"", strings.get(4));
  }

  @Test
  public void testUnpluralize() {
    assertEquals("s", StringUtil.unpluralize("s"));
    assertEquals("z", StringUtil.unpluralize("zs"));
  }

  @Test
  public void testStartsWithConcatenation() {
    assertTrue(StringUtil.startsWithConcatenation("something.withdot", "something", "."));
    assertTrue(StringUtil.startsWithConcatenation("something.withdot", "", "something."));
    assertTrue(StringUtil.startsWithConcatenation("something.", "something", "."));
    assertTrue(StringUtil.startsWithConcatenation("something", "something", ""));
    assertFalse(StringUtil.startsWithConcatenation("something", "something", "."));
    assertFalse(StringUtil.startsWithConcatenation("some", "something", ""));
  }

  @Test
  public void testNaturalCompareTransitivity() {
    String s1 = "#";
    String s2 = "0b";
    String s3 = " 0b";
    assertTrue(StringUtil.naturalCompare(s1, s2) < 0);
    assertTrue(StringUtil.naturalCompare(s2, s3) < 0);
    assertTrue("non-transitive", StringUtil.naturalCompare(s1, s3) < 0);
  }

  @Test
  public void testNaturalCompareStability() {
    assertTrue(StringUtil.naturalCompare("01a1", "1a01") != StringUtil.naturalCompare("1a01", "01a1"));
    assertTrue(StringUtil.naturalCompare("#01A", "# 1A") != StringUtil.naturalCompare("# 1A", "#01A"));
    assertTrue(StringUtil.naturalCompare("aA", "aa") != StringUtil.naturalCompare("aa", "aA"));
  }

  @Test
  public void testNaturalCompare() {

    final List<String> numbers = Arrays.asList("1a000001", "000001a1", "001a0001", "0001A001", "00001a01", "01a00001");
    numbers.sort(NaturalComparator.INSTANCE);
    assertEquals(Arrays.asList("1a000001", "01a00001", "001a0001", "0001A001", "00001a01", "000001a1"), numbers);

    final List<String> test = Arrays.asList("test011", "test10", "test10a", "test010");
    test.sort(NaturalComparator.INSTANCE);
    assertEquals(Arrays.asList("test10", "test10a", "test010", "test011"), test);

    final List<String> strings = Arrays.asList("Test99", "tes0", "test0", "testing", "test", "test99", "test011", "test1", "test 3", "test2", "test10a", "test10", "1.2.10.5", "1.2.9.1");
    strings.sort(NaturalComparator.INSTANCE);
    assertEquals(Arrays.asList("1.2.9.1", "1.2.10.5", "tes0", "test", "test0", "test1", "test2", "test 3", "test10", "test10a", "test011", "Test99", "test99", "testing"), strings);

    final List<String> strings2 = Arrays.asList("t1", "t001", "T2", "T002", "T1", "t2");
    strings2.sort(NaturalComparator.INSTANCE);
    assertEquals(Arrays.asList("T1", "t1", "t001", "T2", "t2", "T002"), strings2);
    assertEquals(1, StringUtil.naturalCompare("7403515080361171695", "07403515080361171694"));
    assertEquals(-14, StringUtil.naturalCompare("_firstField", "myField1"));
    //idea-80853
    final List<String> strings3 = Arrays.asList("C148A_InsomniaCure", "C148B_Escape", "C148C_TersePrincess", "C148D_BagOfMice", "C148E_Porcelain");
    strings3.sort(NaturalComparator.INSTANCE);
    assertEquals(Arrays.asList("C148A_InsomniaCure", "C148B_Escape", "C148C_TersePrincess", "C148D_BagOfMice", "C148E_Porcelain"), strings3);

    final List<String> l = Arrays.asList("a0002", "a0 2", "a001");
    l.sort(NaturalComparator.INSTANCE);
    assertEquals(Arrays.asList("a0 2", "a001", "a0002"), l);
  }

  @Test
  public void testFormatLinks() {
    assertEquals("<a href=\"http://a-b+c\">http://a-b+c</a>", StringUtil.formatLinks("http://a-b+c"));
  }

  @Test
  public void testCopyHeapCharBuffer() {
    String s = "abcde";
    CharBuffer buffer = CharBuffer.allocate(s.length());
    buffer.append(s);
    buffer.rewind();

    assertNotNull(CharArrayUtil.fromSequenceWithoutCopying(buffer));
    assertNotNull(CharArrayUtil.fromSequenceWithoutCopying(buffer.subSequence(0, 5)));
    //assertNull(CharArrayUtil.fromSequenceWithoutCopying(buffer.subSequence(0, 4))); // end index is not checked
    assertNull(CharArrayUtil.fromSequenceWithoutCopying(buffer.subSequence(1, 5)));
    assertNull(CharArrayUtil.fromSequenceWithoutCopying(buffer.subSequence(1, 2)));
  }

  @Test
  public void testTitleCase() {
    assertEquals("Couldn't Connect to Debugger", StringUtil.wordsToBeginFromUpperCase("Couldn't connect to debugger"));
  }

  @Test
  public void testEscapeStringCharacters() {
    assertEquals("\\\"\\n", StringUtil.escapeStringCharacters(3, "\\\"\n", "\"", false, new StringBuilder()).toString());
    assertEquals("\\\"\\n", StringUtil.escapeStringCharacters(2, "\"\n", "\"", false, new StringBuilder()).toString());
    assertEquals("\\\\\\\"\\n", StringUtil.escapeStringCharacters(3, "\\\"\n", "\"", true, new StringBuilder()).toString());
  }

  @Test
  public void testEscapeSlashes() {
    assertEquals("\\/", StringUtil.escapeSlashes("/"));
    assertEquals("foo\\/bar\\foo\\/", StringUtil.escapeSlashes("foo/bar\\foo/"));

    assertEquals("\\\\\\\\server\\\\share\\\\extension.crx", StringUtil.escapeBackSlashes("\\\\server\\share\\extension.crx"));
  }

  @Test
  public void testEscapeQuotes() {
    assertEquals("\\\"", StringUtil.escapeQuotes("\""));
    assertEquals("foo\\\"bar'\\\"", StringUtil.escapeQuotes("foo\"bar'\""));
  }
}
