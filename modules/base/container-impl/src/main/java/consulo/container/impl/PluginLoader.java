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

import com.intellij.openapi.extensions.PluginId;
import consulo.util.nodep.Comparing;
import consulo.util.nodep.io.FileUtilRt;
import consulo.util.nodep.text.StringUtilRt;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
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
public class PluginLoader {
  @NonNls
  public static final String PLUGIN_XML = "plugin.xml";

  @Nullable
  public static IdeaPluginDescriptorImpl loadDescriptor(final File pluginPath, boolean isHeadlessMode, boolean isPreInstalledPath, ContainerLogger containerLogger) {
    return loadDescriptor(pluginPath, PLUGIN_XML, isHeadlessMode, isPreInstalledPath, containerLogger);
  }

  @Nullable
  public static IdeaPluginDescriptorImpl loadDescriptor(final File pluginPath, @NonNls final String fileName, boolean isHeadlessMode, boolean isPreInstalledPath, ContainerLogger containerLogger) {
    IdeaPluginDescriptorImpl descriptor = null;

    if (pluginPath.isDirectory()) {
      File libDir = new File(pluginPath, "lib");
      if (!libDir.isDirectory()) {
        return null;
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
      final Map<PluginId, IdeaPluginDescriptorImpl> descriptors = new HashMap<PluginId, IdeaPluginDescriptorImpl>(descriptor.getOptionalConfigs().size());
      for (Map.Entry<PluginId, String> entry : descriptor.getOptionalConfigs().entrySet()) {
        String optionalDescriptorName = entry.getValue();
        assert !Comparing.equal(fileName, optionalDescriptorName) : "recursive dependency: " + fileName;

        IdeaPluginDescriptorImpl optionalDescriptor = loadDescriptor(pluginPath, optionalDescriptorName, isHeadlessMode, isPreInstalledPath, containerLogger);
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
          containerLogger.info("Cannot find optional descriptor " + optionalDescriptorName);
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
    final ClassLoader classLoader = PluginLoader.class.getClassLoader();
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
  public static IdeaPluginDescriptorImpl loadDescriptorFromJar(@Nonnull File jarFile,
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
          InputStream inputStream = zipFile.getInputStream(entry);

          IdeaPluginDescriptorImpl descriptor = new IdeaPluginDescriptorImpl(pluginPath, isPreInstalledPath);
          descriptor.readExternal(inputStream, zipFile);
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
}
