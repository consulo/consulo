/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.util.io;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import javax.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.function.Function;

/**
 * from kotlin
 */
public class PathKt {
  @Nonnull
  public static InputStream inputStream(@Nonnull Path path) throws IOException {
    return Files.newInputStream(path);
  }

  @javax.annotation.Nullable
  public static InputStream inputStreamIfExists(@Nonnull Path path) throws IOException {
    try {
      return inputStream(path);
    }
    catch (NoSuchFileException e) {
      return null;
    }
  }

  public static boolean exists(@Nonnull Path path) {
    return Files.exists(path);
  }

  public static Path createDirectories(@Nonnull Path path) throws IOException {
    if (!Files.isDirectory(path)) {
      doCreateDirectories(path.toAbsolutePath());
    }

    return path;
  }

  private static void doCreateDirectories(Path path) throws IOException {
    Path parent = path.getParent();

    if (parent != null) {
      if (!Files.isDirectory(parent)) {
        doCreateDirectories(parent);
      }
    }

    Files.createDirectory(path);
  }

  public static OutputStream outputStream(Path path) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      createDirectories(path);
    }

    return Files.newOutputStream(path);
  }

  public static void delete(Path path) throws IOException {
    BasicFileAttributes attributes = basicAttributesIfExists(path);
    if (attributes == null) {
      return;
    }

    try {
      if (attributes.isDirectory()) {
        deleteRecursively(path);
      }
      else {
        Files.delete(path);
      }
    }
    catch (Exception e) {
      FileUtil.delete(path.toFile());
    }
  }

  private static void deleteRecursively(Path path) throws IOException {
    Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        try {
          Files.delete(file);
        }
        catch (Exception e) {
          FileUtil.delete(file.toFile());
        }
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        try {
          Files.delete(dir);
        }
        catch (Exception e) {
          FileUtil.delete(dir.toFile());
        }
        return FileVisitResult.CONTINUE;
      }
    });
  }

  @javax.annotation.Nullable
  public static BasicFileAttributes basicAttributesIfExists(Path path) throws IOException {
    try {
      return Files.readAttributes(path, BasicFileAttributes.class);
    }
    catch (NoSuchFileException e) {
      return null;
    }
  }

  public static String sanitizeFileName(String name) {
    return sanitizeFileName(name, "_", true);
  }

  public static String sanitizeFileName(String name, boolean isTruncate) {
    return sanitizeFileName(name, "_", isTruncate);
  }

  private static Set<Character> illegalChars = ContainerUtil.newHashSet('/', '\\', '?', '<', '>', ':', '*', '|', '"', ':');

  // https://github.com/parshap/node-sanitize-filename/blob/master/index.js
  public static String sanitizeFileName(String name, String replacement, boolean isTruncate) {
    StringBuilder result = null;
    int last = 0;
    int lenght = name.length();

    for (int i = 0; i < lenght; i++) {
      char c = name.charAt(i);

      if (!illegalChars.contains(c) && !Character.isISOControl(c)) {
        continue;
      }

      if (result == null) {
        result = new StringBuilder();
      }

      if (last < i) {
        result.append(name, last, i);
      }

      if (replacement != null) {
        result.append(replacement);
      }

      last = i + 1;
    }

    Function<String, String> truncateFileName = it -> {
      if (isTruncate) {
        return it.substring(0, Math.min(lenght, 255));
      }
      else {
        return it;
      }
    };


    if (result == null) {
      return truncateFileName.apply(name);
    }

    if (last < lenght) {
      result.append(name, last, lenght);
    }

    return truncateFileName.apply(result.toString());
  }
}
