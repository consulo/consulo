/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.vcs;

import com.intellij.notification.Notification;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.containers.ContainerUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;

public class VcsTestUtil {
  public static VirtualFile createFile(@Nonnull Project project, @Nonnull final VirtualFile parent, @Nonnull final String name,
                                       @javax.annotation.Nullable final String content) {
    return new WriteCommandAction<VirtualFile>(project) {
      @Override
      protected void run(@Nonnull Result<VirtualFile> result) throws Throwable {
        VirtualFile file = parent.createChildData(this, name);
        if (content != null) {
          file.setBinaryContent(content.getBytes(StandardCharsets.UTF_8));
        }
        result.setResult(file);
      }
    }.execute().throwException().getResultObject();
  }

  /**
   * Creates directory inside a write action and returns the resulting reference to it.
   * If the directory already exists, does nothing.
   *
   * @param parent Parent directory.
   * @param name   Name of the directory.
   * @return reference to the created or already existing directory.
   */
  public static VirtualFile findOrCreateDir(@Nonnull final Project project, @Nonnull final VirtualFile parent, @Nonnull final String name) {
    return new WriteCommandAction<VirtualFile>(project) {
      @Override
      protected void run(@Nonnull Result<VirtualFile> result) throws Throwable {
        VirtualFile dir = parent.findChild(name);
        if (dir == null) {
          dir = parent.createChildDirectory(this, name);
        }
        result.setResult(dir);
      }
    }.execute().throwException().getResultObject();
  }

  public static void renameFileInCommand(@Nonnull Project project, @Nonnull final VirtualFile file, @Nonnull final String newName) {
    new WriteCommandAction.Simple(project) {
      @Override
      protected void run() throws Throwable {
        try {
          file.rename(this, newName);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }.execute().throwException();
  }

  public static void deleteFileInCommand(@Nonnull Project project, @Nonnull final VirtualFile file) {
    new WriteCommandAction.Simple(project) {
      @Override
      protected void run() throws Throwable {
        try {
          file.delete(this);
        }
        catch(IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    }.execute();
  }

  public static void editFileInCommand(@Nonnull Project project, @Nonnull final VirtualFile file, @Nonnull final String newContent) {
    assertTrue(file.isValid());
    file.getTimeStamp();
    new WriteCommandAction.Simple(project) {
      @Override
      protected void run() throws Throwable {
        try {
          final long newTs = Math.max(System.currentTimeMillis(), file.getTimeStamp() + 1100);
          file.setBinaryContent(newContent.getBytes(), -1, newTs);
          final File file1 = new File(file.getPath());
          FileUtil.writeToFile(file1, newContent.getBytes());
          file.refresh(false, false);
          assertTrue(file1 + " / " + newTs, file1.setLastModified(newTs));
        }
        catch(IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    }.execute();
  }

  @Nonnull
  public static VirtualFile copyFileInCommand(@Nonnull Project project, @Nonnull final VirtualFile file,
                                              @Nonnull final VirtualFile newParent, @Nonnull final String newName) {
    return new WriteCommandAction<VirtualFile>(project) {
      @Override
      protected void run(@Nonnull Result<VirtualFile> result) throws Throwable {
        try {
          result.setResult(file.copy(this, newParent, newName));
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }.execute().getResultObject();
  }

  public static void moveFileInCommand(@Nonnull Project project, @Nonnull final VirtualFile file, @Nonnull final VirtualFile newParent) {
    new WriteCommandAction.Simple(project) {
      @Override
      protected void run() throws Throwable {
        try {
          file.move(this, newParent);
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }.execute();
  }

  public static <T> void assertEqualCollections(@Nonnull String message, @Nonnull Collection<T> actual, @Nonnull Collection<T> expected) {
    if (!StringUtil.isEmptyOrSpaces(message) && !message.endsWith(":") && !message.endsWith(": ")) {
      message += ": ";
    }
    if (actual.size() != expected.size()) {
      fail(message + "Collections don't have the same size. " + stringifyActualExpected(actual, expected));
    }
    for (T act : actual) {
      if (!expected.contains(act)) {
        fail(message + "Unexpected object " + act + stringifyActualExpected(actual, expected));
      }
    }
    // backwards is needed for collections which may contain duplicates, e.g. Lists.
    for (T exp : expected) {
      if (!actual.contains(exp)) {
        fail(message + "Object " + exp + " not found in actual collection." + stringifyActualExpected(actual, expected));
      }
    }
  }

  public static <T> void assertEqualCollections(@Nonnull Collection<T> actual, @Nonnull Collection<T> expected) {
    assertEqualCollections("", actual, expected);
  }

  /**
   * Testng compares by iterating over 2 collections, but it won't work for sets which may have different order.
   */
  public static <T, E> void assertEqualCollections(@Nonnull Collection<? extends T> actual,
                                                   @Nonnull Collection<? extends E> expected,
                                                   @Nonnull EqualityChecker<T, E> equalityChecker) {
    if (actual.size() != expected.size()) {
      fail("Collections don't have the same size. " + stringifyActualExpected(actual, expected));
    }
    for (T act : actual) {
      if (!contains2(expected, act, equalityChecker)) {
        fail("Unexpected object " + act + stringifyActualExpected(actual, expected));
      }
    }
    // backwards is needed for collections which may contain duplicates, e.g. Lists.
    for (E exp : expected) {
      if (!contains(actual, exp, equalityChecker)) {
        fail("Object " + exp + " not found in actual collection." + stringifyActualExpected(actual, expected));
      }
    }
  }

  private static <T, E> boolean contains(@Nonnull Collection<? extends T> collection,
                                         @Nonnull E object,
                                         @Nonnull EqualityChecker<T, E> equalityChecker) {
    for (T t : collection) {
      if (equalityChecker.areEqual(t, object)) {
        return true;
      }
    }
    return false;
  }

  private static <T, E> boolean contains2(@Nonnull Collection<? extends E> collection,
                                          @Nonnull T object,
                                          @Nonnull EqualityChecker<T, E> equalityChecker) {
    for (E e : collection) {
      if (equalityChecker.areEqual(object, e)) {
        return true;
      }
    }
    return false;
  }

  public interface EqualityChecker<T, E> {
    boolean areEqual(T actual, E expected);
  }

  @Nonnull
  public static String stringifyActualExpected(@Nonnull Object actual, @Nonnull Object expected) {
    return "\nExpected:\n" + expected + "\nActual:\n" + actual;
  }

  @Nonnull
  public static String toAbsolute(@Nonnull String relPath, @Nonnull Project project) {
    new File(toAbsolute(Collections.singletonList(relPath), project).get(0)).mkdir();
    return toAbsolute(Collections.singletonList(relPath), project).get(0);
  }

  @Nonnull
  public static List<String> toAbsolute(@Nonnull Collection<String> relPaths, @Nonnull final Project project) {
    return ContainerUtil.map2List(relPaths, s -> {
      try {
        return FileUtil.toSystemIndependentName((new File(project.getBasePath() + "/" + s).getCanonicalPath()));
      }
      catch (IOException e) {
        e.printStackTrace();
        return "";
      }
    });
  }

  public static void assertNotificationShown(@Nonnull Project project, @Nullable Notification expected) {
    if (expected != null) {
      Notification actualNotification =
              ((TestVcsNotifier)VcsNotifier.getInstance(project)).getLastNotification();
      assertNotNull("No notification was shown", actualNotification);
      assertEquals("Notification has wrong title", expected.getTitle(), actualNotification.getTitle());
      assertEquals("Notification has wrong type", expected.getType(), actualNotification.getType());
      assertEquals("Notification has wrong content", adjustTestContent(expected.getContent()), actualNotification.getContent());
    }
  }

  // we allow more spaces and line breaks in tests to make them more readable.
  // After all, notifications display html, so all line breaks and extra spaces are ignored.
  private static String adjustTestContent(@Nonnull String s) {
    StringBuilder res = new StringBuilder();
    String[] splits = s.split("\n");
    for (String split : splits) {
      res.append(split.trim());
    }

    return res.toString();
  }

  public static String getTestDataPath() {
    return PlatformTestUtil.getCommunityPath() + "/platform/vcs-tests/testData";
  }
}
