/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.util.io;

import consulo.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class JarUtil {
  private static final Logger LOG = Logger.getInstance(JarUtil.class);

  /**
   * Returns true if the given .jar file exists and contains the given class.
   */
  public static boolean containsClass(@Nonnull String jarPath, @Nonnull String className) {
    return containsClass(new File(jarPath), className);
  }

  /**
   * Returns true if the given .jar file exists and contains the given class.
   */
  public static boolean containsClass(@Nonnull File file, String className) {
    String entryPath = className.replace('.', '/') + ".class";
    return containsEntry(file, entryPath);
  }

  /**
   * Returns true if the given .jar file exists and contains the entry.
   */
  public static boolean containsEntry(File file, String entryPath) {
    if (file.canRead()) {
      try {
        JarFile jarFile = new JarFile(file);
        try {
          return jarFile.getEntry(entryPath) != null;
        }
        finally {
          jarFile.close();
        }
      }
      catch (IOException ignored) { }
    }

    return false;
  }

  /**
   * Returns attribute value from a manifest main section,
   * or null if missing or a file does not contain a manifest.
   */
  @Nullable
  public static String getJarAttribute(@Nonnull File file, @Nonnull Attributes.Name attribute) {
    return getJarAttributeImpl(file, null, attribute);
  }

  /**
   * Returns attribute value from a given manifest section,
   * or null if missing or a file does not contain a manifest.
   */
  @Nullable
  public static String getJarAttribute(@Nonnull File file, @Nonnull String entryName, @Nonnull Attributes.Name attribute) {
    return getJarAttributeImpl(file, entryName, attribute);
  }

  private static String getJarAttributeImpl(@Nonnull File file, @Nullable String entryName, @Nonnull Attributes.Name attribute) {
    if (file.canRead()) {
      try {
        JarFile jarFile = new JarFile(file);
        try {
          Manifest manifest = jarFile.getManifest();
          if (manifest != null) {
            Attributes attributes = entryName != null ? manifest.getAttributes(entryName) : manifest.getMainAttributes();
            return attributes.getValue(attribute);
          }
        }
        finally {
          jarFile.close();
        }
      }
      catch (IOException e) {
        LOG.debug(e);
      }
    }

    return null;
  }

  /**
   * Loads archive entry as Java properties.
   * Returns loaded instance, or null if requested entry is missed or invalid.
   */
  @Nullable
  public static Properties loadProperties(@Nonnull File file, @Nonnull String entryName) {
    if (file.canRead()) {
      try {
        ZipFile zipFile = new ZipFile(file);
        try {
          ZipEntry entry = zipFile.getEntry(entryName);
          if (entry != null) {
            Properties properties = new Properties();
            properties.load(zipFile.getInputStream(entry));
            return properties;
          }
        }
        finally {
          zipFile.close();
        }
      }
      catch (IOException e) {
        LOG.debug(e);
      }
    }

    return null;
  }
}
