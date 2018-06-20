/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */

/*
 * Created by IntelliJ IDEA.
 * User: Anna.Kozlova
 * Date: 13-Sep-2006
 * Time: 16:37:57
 */
package com.intellij.ide.packageSets;

import com.intellij.psi.search.scope.packageSet.FilePatternPackageSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PatternRegexpConvertorTest {
  @Test
  public void testConvertToRegexp() throws Exception {
    assertEquals("a\\.[^\\.]*", FilePatternPackageSet.convertToRegexp("a.*", '.'));
    assertEquals("a\\.(.*\\.)?[^\\.]*", FilePatternPackageSet.convertToRegexp("a..*", '.'));
    assertEquals("a\\/[^\\/]*", FilePatternPackageSet.convertToRegexp("a/*", '/'));
    assertEquals("a\\/(.*\\/)?[^\\/]*", FilePatternPackageSet.convertToRegexp("a//*", '/'));
    assertEquals("[^\\.]*", FilePatternPackageSet.convertToRegexp("*", '.'));
  }
}