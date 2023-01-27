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
package consulo.container.impl;

import consulo.util.nodep.ArrayUtilRt;
import consulo.util.nodep.io.FileUtilRt;

import java.io.*;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public class PluginDescriptorLoader {
  public static final String PLUGIN_XML = "plugin.xml";

  public static PluginDescriptorImpl loadDescriptor(final File pluginPath,
                                                    boolean isPreInstalledPath,
                                                    ContainerLogger containerLogger) {
    return loadDescriptor(pluginPath, PLUGIN_XML, isPreInstalledPath, containerLogger);
  }

  public static PluginDescriptorImpl loadDescriptor(final File pluginPath,
                                                    final String fileName,
                                                    boolean isPreInstalledPath,
                                                    ContainerLogger containerLogger) {
    if (!pluginPath.isDirectory()) {
      // single jar not supported
      return null;
    }

    File pluginMetaInfoDir = new File(pluginPath, "META-INF");
    if (pluginMetaInfoDir.exists()) {
      File icon = new File(pluginMetaInfoDir, "pluginIcon.svg");
      File iconDark = new File(pluginMetaInfoDir, "pluginIcon_dark.svg");

      byte[] iconBytes = ArrayUtilRt.EMPTY_BYTE_ARRAY, darkIconBytes = ArrayUtilRt.EMPTY_BYTE_ARRAY;

      if (icon.exists()) {
        try {
          iconBytes = Files.readAllBytes(icon.toPath());
        }
        catch (Throwable e) {
          containerLogger.error("Fail to load " + icon, e);
        }
      }

      if (iconDark.exists()) {
        try {
          darkIconBytes = Files.readAllBytes(iconDark.toPath());
        }
        catch (Throwable e) {
          containerLogger.error("Fail to load " + iconDark, e);
        }
      }

      PluginDescriptorImpl descriptor = new PluginDescriptorImpl(pluginPath, iconBytes, darkIconBytes, isPreInstalledPath);
      File pluginXmlFile = new File(pluginMetaInfoDir, PLUGIN_XML);
      try (FileInputStream stream = new FileInputStream(pluginXmlFile)) {
        descriptor.readExternal(stream, containerLogger);
      }
      catch (Throwable e) {
        containerLogger.error("Fail to load " + pluginXmlFile, e);
      }
      return descriptor;
    }

    File libDir = new File(pluginPath, "lib");
    if (!libDir.isDirectory()) {
      return null;
    }

    File[] markerFiles = libDir.listFiles(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.getName().endsWith(".jar.marker");
      }
    });

    if (markerFiles != null && markerFiles.length == 1) {
      String simpleJarFile = markerFiles[0].getName().replace(".jar.marker", ".jar");
      File jarFile = new File(libDir, simpleJarFile);
      return loadDescriptorFromJar(jarFile, pluginPath, fileName, isPreInstalledPath, containerLogger);
    }

    final File[] files = libDir.listFiles();
    if (files == null || files.length == 0) {
      return null;
    }

    for (final File f : files) {
      if (FileUtilRt.isJarOrZip(f)) {
        PluginDescriptorImpl descriptor = loadDescriptorFromJar(f, pluginPath, fileName, isPreInstalledPath, containerLogger);
        if (descriptor != null) {
          return descriptor;
        }
      }
    }

    return null;
  }

  public static PluginDescriptorImpl loadDescriptorFromJar(File jarFile,
                                                           File pluginPath,
                                                           String fileName,
                                                           boolean isPreInstalledPath,
                                                           ContainerLogger logger) {
    try {
      ZipFile zipFile = new ZipFile(jarFile.getPath());
      try {
        ZipEntry entry = zipFile.getEntry("META-INF/" + fileName);
        if (entry != null) {
          byte[] iconBytes = ArrayUtilRt.EMPTY_BYTE_ARRAY;
          byte[] darkIconBytes = ArrayUtilRt.EMPTY_BYTE_ARRAY;

          ZipEntry pluginIconSvg = zipFile.getEntry("META-INF/pluginIcon.svg");
          if (pluginIconSvg != null) {
            iconBytes = loadFromStream(zipFile.getInputStream(pluginIconSvg));
          }

          pluginIconSvg = zipFile.getEntry("META-INF/pluginIcon_dark.svg");
          if (pluginIconSvg != null) {
            darkIconBytes = loadFromStream(zipFile.getInputStream(pluginIconSvg));
          }

          InputStream inputStream = zipFile.getInputStream(entry);

          PluginDescriptorImpl descriptor = new PluginDescriptorImpl(pluginPath, iconBytes, darkIconBytes, isPreInstalledPath);
          descriptor.readExternal(inputStream, logger);
          return descriptor;
        }
      }
      finally {
        try {
          zipFile.close();
        }
        catch (IOException ignored) {
        }
      }
    }
    catch (Throwable e) {
      logger.info("Cannot load " + jarFile, e);
    }

    return null;
  }

  private static byte[] loadFromStream(InputStream inputStream) throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      copyStreamContent(inputStream, outputStream);
    }
    finally {
      inputStream.close();
    }
    return outputStream.toByteArray();
  }

  private static int copyStreamContent(InputStream inputStream, OutputStream outputStream) throws IOException {
    final byte[] buffer = new byte[10 * 1024];
    int count;
    int total = 0;
    while ((count = inputStream.read(buffer)) > 0) {
      outputStream.write(buffer, 0, count);
      total += count;
    }
    return total;
  }
}
