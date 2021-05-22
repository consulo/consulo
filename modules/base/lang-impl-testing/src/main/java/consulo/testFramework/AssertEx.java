/*
 * Copyright 2013-2019 consulo.io
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
package consulo.testFramework;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.FileComparisonFailure;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import consulo.util.lang.Comparing;
import junit.framework.AssertionFailedError;
import org.jetbrains.annotations.NonNls;
import org.junit.Assert;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2019-12-07
 */
public class AssertEx extends Assert {
  protected static boolean OVERWRITE_TESTDATA = false;

  public static <T> void assertOrderedCollection(T[] collection, @Nonnull Consumer<T>... checkers) {
    assertNotNull(collection);
    assertOrderedCollection(Arrays.asList(collection), checkers);
  }

  public static <T> void assertSameElements(T[] collection, T... expected) {
    assertSameElements(Arrays.asList(collection), expected);
  }

  public static <T> void assertSameElements(Collection<? extends T> collection, T... expected) {
    assertSameElements(collection, Arrays.asList(expected));
  }

  public static <T> void assertSameElements(Collection<? extends T> collection, Collection<T> expected) {
    assertSameElements(null, collection, expected);
  }

  public static <T> void assertSameElements(String message, Collection<? extends T> collection, Collection<T> expected) {
    assertNotNull(collection);
    assertNotNull(expected);
    if (collection.size() != expected.size() || !new HashSet<T>(expected).equals(new HashSet<T>(collection))) {
      assertEquals(message, toString(expected, "\n"), toString(collection, "\n"));
      assertEquals(message, new HashSet<T>(expected), new HashSet<T>(collection));
    }
  }

  public <T> void assertContainsOrdered(Collection<? extends T> collection, T... expected) {
    assertContainsOrdered(collection, Arrays.asList(expected));
  }

  public <T> void assertContainsOrdered(Collection<? extends T> collection, Collection<T> expected) {
    ArrayList<T> copy = new ArrayList<T>(collection);
    copy.retainAll(expected);
    assertOrderedEquals(toString(collection), copy, expected);
  }

  public static <T> void assertOrderedEquals(final Iterable<? extends T> actual, final Collection<? extends T> expected) {
    assertOrderedEquals(null, actual, expected);
  }

  public static <T> void assertOrderedEquals(final String erroMsg, final Iterable<? extends T> actual, final Collection<? extends T> expected) {
    ArrayList<T> list = new ArrayList<T>();
    for (T t : actual) {
      list.add(t);
    }
    if (!list.equals(new ArrayList<T>(expected))) {
      String expectedString = toString(expected);
      String actualString = toString(actual);
      assertEquals(erroMsg, expectedString, actualString);
      fail("Warning! 'toString' do not reflect the difference.\nExpected: " + expectedString + "\nActual: " + actualString);
    }
  }

  public <T> void assertContainsElements(Collection<? extends T> collection, T... expected) {
    assertContainsElements(collection, Arrays.asList(expected));
  }

  public <T> void assertContainsElements(Collection<? extends T> collection, Collection<T> expected) {
    ArrayList<T> copy = new ArrayList<T>(collection);
    copy.retainAll(expected);
    assertSameElements(toString(collection), copy, expected);
  }

  public static String toString(Object[] collection, String separator) {
    return toString(Arrays.asList(collection), separator);
  }

  public <T> void assertDoesntContain(Collection<? extends T> collection, T... notExpected) {
    assertDoesntContain(collection, Arrays.asList(notExpected));
  }

  public <T> void assertDoesntContain(Collection<? extends T> collection, Collection<T> notExpected) {
    ArrayList<T> expected = new ArrayList<T>(collection);
    expected.removeAll(notExpected);
    assertSameElements(collection, expected);
  }

  public static String toString(Collection<?> collection, String separator) {
    List<String> list = ContainerUtil.map2List(collection, (Function<Object, String>)String::valueOf);
    Collections.sort(list);
    StringBuilder builder = new StringBuilder();
    boolean flag = false;
    for (final String o : list) {
      if (flag) {
        builder.append(separator);
      }
      builder.append(o);
      flag = true;
    }
    return builder.toString();
  }

  public static <T> void assertOrderedCollection(Collection<? extends T> collection, Consumer<T>... checkers) {
    assertNotNull(collection);
    if (collection.size() != checkers.length) {
      fail(toString(collection));
    }
    int i = 0;
    for (final T actual : collection) {
      try {
        checkers[i].accept(actual);
      }
      catch (AssertionFailedError e) {
        System.out.println(i + ": " + actual);
        throw e;
      }
      i++;
    }
  }

  public static <T> void assertUnorderedCollection(T[] collection, Consumer<T>... checkers) {
    assertUnorderedCollection(Arrays.asList(collection), checkers);
  }

  public static <T> void assertUnorderedCollection(Collection<? extends T> collection, Consumer<T>... checkers) {
    assertNotNull(collection);
    if (collection.size() != checkers.length) {
      fail(toString(collection));
    }
    Set<Consumer<T>> checkerSet = new HashSet<Consumer<T>>(Arrays.asList(checkers));
    int i = 0;
    Throwable lastError = null;
    for (final T actual : collection) {
      boolean flag = true;
      for (final Consumer<T> condition : checkerSet) {
        Throwable error = accepts(condition, actual);
        if (error == null) {
          checkerSet.remove(condition);
          flag = false;
          break;
        }
        else {
          lastError = error;
        }
      }
      if (flag) {
        lastError.printStackTrace();
        fail("Incorrect element(" + i + "): " + actual);
      }
      i++;
    }
  }

  private static <T> Throwable accepts(final Consumer<T> condition, final T actual) {
    try {
      condition.accept(actual);
      return null;
    }
    catch (Throwable e) {
      return e;
    }
  }

  public static void assertSameLinesWithFile(String filePath, String actualText) {
    String fileText;
    try {
      if (OVERWRITE_TESTDATA) {
        FileUtil.writeToFile(new File(filePath), actualText);
        System.out.println("File " + filePath + " created.");
      }
      fileText = FileUtil.loadFile(new File(filePath));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    String expected = StringUtil.convertLineSeparators(fileText.trim());
    String actual = StringUtil.convertLineSeparators(actualText.trim());
    if (!Comparing.equal(expected, actual)) {
      throw new FileComparisonFailure(null, expected, actual, filePath);
    }
  }

  public static void assertSameLines(String expected, String actual) {
    String expectedText = StringUtil.convertLineSeparators(expected.trim());
    String actualText = StringUtil.convertLineSeparators(actual.trim());
    assertEquals(expectedText, actualText);
  }

  @NonNls
  public static String toString(Iterable<?> collection) {
    if (!collection.iterator().hasNext()) {
      return "<empty>";
    }

    final StringBuilder builder = new StringBuilder();
    for (final Object o : collection) {
      if (o instanceof HashSet) {
        builder.append(new TreeSet<Object>((HashSet)o));
      }
      else {
        builder.append(o);
      }
      builder.append("\n");
    }
    return builder.toString();
  }
}
