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
package consulo.localize.impl;

import com.intellij.CommonBundle;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.EventDispatcher;
import com.intellij.util.io.URLUtil;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeManagerListener;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public class LocalizeManagerImpl extends LocalizeManager {
  private static final Logger LOG = Logger.getInstance(LocalizeManagerImpl.class);

  private static final Locale ourDefaultLocale = Locale.ENGLISH;

  private final AtomicBoolean myInitialized = new AtomicBoolean();

  public static final String LOCALIZE_LIBRARY_MARKER = "localize/id.txt";

  private final Map<Locale, Map<String, LocalizeFileState>> myLocalizes = new HashMap<>();

  private Locale myCurrentLocale = ourDefaultLocale;

  private final EventDispatcher<LocalizeManagerListener> myEventDispatcher = EventDispatcher.create(LocalizeManagerListener.class);

  private final AtomicLong myModificationCount = new AtomicLong();

  public void initialize(@Nullable List<String> files) {
    if (myInitialized.compareAndSet(false, true)) {
      if(files == null) {
        return;
      }

      for (String file : files) {
        try {
          analyzeLibraryJar(file);
        }
        catch (IOException e) {
          LOG.error("Fail to analyze library from url: " + file, e);
        }
      }
      myModificationCount.incrementAndGet();
    }
  }

  private void analyzeLibraryJar(String filePath) throws IOException {
    String localeString = null;
    File jarFile = new File(filePath);

    Map<String, LocalizeFileState> localizeFiles = new HashMap<>();

    try (ZipFile zipFile = new ZipFile(jarFile)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = entries.nextElement();

        final String name = zipEntry.getName();

        if (LOCALIZE_LIBRARY_MARKER.equals(name)) {
          try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
            byte[] bytes = StreamUtil.loadFromStream(inputStream);

            localeString = new String(bytes, StandardCharsets.UTF_8);
          }
        }
        else if (name.startsWith("localize/") && name.endsWith(".yaml")) {
          String pluginId = name.substring(name.indexOf('/') + 1, name.length());
          pluginId = pluginId.substring(0, pluginId.lastIndexOf('/'));
          pluginId = pluginId.replace('/', '.');

          URL localizeFileUrl = URLUtil.getJarEntryURL(jarFile, name);

          String fileName = StringUtil.getShortName(name, '/');
          String id = FileUtilRt.getNameWithoutExtension(fileName);

          String localizeId = pluginId + "." + id;
          localizeFiles.put(localizeId, new LocalizeFileState(localizeId, localizeFileUrl));
        }
      }
    }

    if (StringUtil.isEmptyOrSpaces(localeString)) {
      LOG.warn("There no locale file inside: " + filePath);
      return;
    }

    Locale locale = buildLocale(localeString);
    Map<String, LocalizeFileState> mapByLocalizeId = myLocalizes.computeIfAbsent(locale, l -> new HashMap<>());

    mapByLocalizeId.putAll(localizeFiles);
  }

  @Nonnull
  private Locale buildLocale(String fullId) {
    StringTokenizer tokenizer = new StringTokenizer(fullId, "_");
    String language = tokenizer.nextToken();
    String country = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";
    String variant = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "";

    return new Locale(language, country, variant);
  }

  @Nonnull
  @Override
  public Set<Locale> getAvaliableLocales() {
    return Collections.unmodifiableSet(myLocalizes.keySet());
  }

  @Override
  public void setLocale(@Nonnull Locale locale, boolean fireEvents) {
    Locale oldLocale = myCurrentLocale;

    myCurrentLocale = locale;

    myModificationCount.incrementAndGet();

    if(fireEvents) {
      myEventDispatcher.getMulticaster().localeChanged(oldLocale, locale);
    }
  }

  @Nonnull
  @Override
  public Locale getLocale() {
    return myCurrentLocale;
  }

  @Override
  public boolean isDefaultLocale() {
    return ourDefaultLocale.equals(myCurrentLocale);
  }

  @Override
  public void addListener(@Nonnull LocalizeManagerListener listener, @Nonnull Disposable disposable) {
    myEventDispatcher.addListener(listener, disposable);
  }

  @Override
  public long getModificationCount() {
    return myModificationCount.get();
  }

  @Nonnull
  @Override
  public Locale parseLocale(@Nonnull String localeText) {
    try {
      return buildLocale(localeText);
    }
    catch (Exception e) {
      LOG.error(e);
      return ourDefaultLocale;
    }
  }

  @Nonnull
  @Override
  public LocalizeValue fromStringKey(@Nonnull String localizeKeyInfo) {
    List<String> values = StringUtil.split(localizeKeyInfo, "@");
    if(values.size() != 2) {
      return LocalizeValue.of(localizeKeyInfo);
    }

    LocalizeKey localizeKey = LocalizeKey.of(values.get(0), values.get(1));
    return localizeKey.getValue();
  }

  @Nonnull
  @Override
  public String getUnformattedText(@Nonnull LocalizeKey key) {
    if (StringUtil.isEmptyOrSpaces(key.getKey())) {
      return "";
    }

    String value = getValue(key, myCurrentLocale);
    if (value != null) {
      return value;
    }

    value = getValue(key, ourDefaultLocale);
    if (value != null) {
      return value;
    }

    LOG.warn("Can't find localize value: " + key + ", current locale: " + myCurrentLocale);
    return key.toString();
  }

  @Nonnull
  @Override
  public String formatText(String unformattedText, Object... arg) {
    return CommonBundle.format(unformattedText, arg);
  }

  @Nullable
  private String getValue(LocalizeKey key, Locale locale) {
    Map<String, LocalizeFileState> map = myLocalizes.get(locale);
    if (map != null) {
      LocalizeFileState fileInfo = map.get(key.getLocalizeId());
      if (fileInfo != null) {
        String value = fileInfo.getValue(key);
        if (value != null) {
          return value;
        }
      }
    }

    return null;
  }
}
