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

import consulo.container.plugin.PluginId;
import consulo.util.nodep.ArrayUtilRt;
import consulo.util.nodep.Comparing;
import consulo.util.nodep.io.FileUtilRt;
import consulo.util.nodep.text.StringUtilRt;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author VISTALL
 * @since 2019-07-25
 */
public class PluginDescriptorLoader {
  public static final String PLUGIN_XML = "plugin.xml";

  @Nullable
  public static PluginDescriptorImpl loadDescriptor(final File pluginPath, boolean isHeadlessMode, boolean isPreInstalledPath, ContainerLogger containerLogger) {
    return loadDescriptor(pluginPath, PLUGIN_XML, isHeadlessMode, isPreInstalledPath, containerLogger);
  }

  @Nullable
  public static PluginDescriptorImpl loadDescriptor(final File pluginPath, final String fileName, boolean isHeadlessMode, boolean isPreInstalledPath, ContainerLogger containerLogger) {
    PluginDescriptorImpl descriptor = null;

    if (pluginPath.isDirectory()) {
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
        return loadDescriptorFromJar(jarFile, pluginPath, fileName, isHeadlessMode, isPreInstalledPath, containerLogger);
      }

      final File[] files = libDir.listFiles();
      if (files == null || files.length == 0) {
        return null;
      }

      for (final File f : files) {
        if (FileUtilRt.isJarOrZip(f)) {
          descriptor = loadDescriptorFromJar(f, pluginPath, fileName, isHeadlessMode, isPreInstalledPath, containerLogger);
          if (descriptor != null) {
            break;
          }
        }
      }
    }

    if (descriptor != null && descriptor.getOptionalConfigs() != null && !descriptor.getOptionalConfigs().isEmpty()) {
      final Map<PluginId, PluginDescriptorImpl> descriptors = new HashMap<PluginId, PluginDescriptorImpl>(descriptor.getOptionalConfigs().size());
      for (Map.Entry<PluginId, String> entry : descriptor.getOptionalConfigs().entrySet()) {
        String optionalDescriptorName = entry.getValue();
        assert !Comparing.equal(fileName, optionalDescriptorName) : "recursive dependency: " + fileName;

        PluginDescriptorImpl optionalDescriptor = loadDescriptor(pluginPath, optionalDescriptorName, isHeadlessMode, isPreInstalledPath, containerLogger);
        if (optionalDescriptor == null && !FileUtilRt.isJarOrZip(pluginPath)) {
          for (URL url : getClassLoaderUrls()) {
            if ("file".equals(url.getProtocol())) {
              optionalDescriptor = loadDescriptor(new File(decodeUrl(url.getFile())), optionalDescriptorName, isHeadlessMode, isPreInstalledPath, containerLogger);
              if (optionalDescriptor != null) {
                break;
              }
            }
          }
        }
        if (optionalDescriptor != null) {
          descriptors.put(entry.getKey(), optionalDescriptor);
        }
        else {
          containerLogger.info("Cannot find optional descriptor " + optionalDescriptorName + ", Plugin: " + descriptor);
        }
      }
      descriptor.setOptionalDescriptors(descriptors);
    }

    return descriptor;
  }

  private static String decodeUrl(String file) {
    String quotePluses = StringUtilRt.replace(file, "+", "%2B");
    try {
      return URLDecoder.decode(quotePluses, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  static Collection<URL> getClassLoaderUrls() {
    final ClassLoader classLoader = PluginDescriptorLoader.class.getClassLoader();
    final Class<? extends ClassLoader> aClass = classLoader.getClass();
    try {
      return (List<URL>)aClass.getMethod("getUrls").invoke(classLoader);
    }
    catch (IllegalAccessException ignored) {
    }
    catch (InvocationTargetException ignored) {
    }
    catch (NoSuchMethodException ignored) {
    }

    if (classLoader instanceof URLClassLoader) {
      return Arrays.asList(((URLClassLoader)classLoader).getURLs());
    }

    return Collections.emptyList();
  }


  @Nullable
  public static PluginDescriptorImpl loadDescriptorFromJar(@Nonnull File jarFile,
                                                           @Nonnull File pluginPath,
                                                           @Nonnull String fileName,
                                                           boolean isHeadlessMode,
                                                           boolean isPreInstalledPath,
                                                           @Nonnull ContainerLogger logger) {
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
          descriptor.readExternal(inputStream, zipFile, logger);
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
