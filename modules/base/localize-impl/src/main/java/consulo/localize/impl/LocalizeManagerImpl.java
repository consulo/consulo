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

import consulo.disposer.Disposable;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeManagerListener;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.proxy.EventDispatcher;
import consulo.util.io.URLUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
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

  private static final Locale ourDefaultLocale = Locale.US;

  private final AtomicBoolean myInitialized = new AtomicBoolean();

  public static final String LOCALIZE_DIRECTORY = "LOCALIZE-LIB/";

  private final Map<Locale, Map<String, LocalizeFileState>> myLocalizes = new HashMap<>();

  private Locale myCurrentLocale = ourDefaultLocale;

  private final EventDispatcher<LocalizeManagerListener> myEventDispatcher = EventDispatcher.create(LocalizeManagerListener.class);

  private final AtomicLong myModificationCount = new AtomicLong();

  public void initialize(@Nullable Set<String> files) {
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
    File jarFile = new File(filePath);

    // locale <localize id, state>
    Map<String, Map<String, LocalizeFileState>> localizeFiles = new HashMap<>();

    try (ZipFile zipFile = new ZipFile(jarFile)) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry zipEntry = entries.nextElement();

        final String name = zipEntry.getName();

        if (name.startsWith(LOCALIZE_DIRECTORY) && name.endsWith(".yaml")) {
          String fullFilePath = name.substring(LOCALIZE_DIRECTORY.length(), name.length());

          String locale = fullFilePath.substring(0, fullFilePath.indexOf('/'));

          // -5 - its '.yaml' prefix
          String localizeId = fullFilePath.substring(locale.length() + 1, fullFilePath.length() - 5);

          Map<String, LocalizeFileState> map = localizeFiles.computeIfAbsent(locale, l -> new HashMap<>());

          URL localizeFileUrl = URLUtil.getJarEntryURL(jarFile, name);

          map.put(localizeId, new LocalizeFileState(localizeId, localizeFileUrl));
        }
      }
    }

    for (Map.Entry<String, Map<String, LocalizeFileState>> entry : localizeFiles.entrySet()) {
      String localeString = entry.getKey();
      Map<String, LocalizeFileState> states = entry.getValue();

      Locale locale = buildLocale(localeString);

      Map<String, LocalizeFileState> mapByLocalizeId = myLocalizes.computeIfAbsent(locale, l -> new HashMap<>());

      mapByLocalizeId.putAll(states);
    }
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
    if (!myInitialized.get()) {
      throw new IllegalArgumentException("not initialized");
    }

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
    return format(unformattedText, arg);
  }

  @Nonnull
  private static String format(@Nonnull String value, @Nonnull Object... params) {
    if (params.length > 0 && value.indexOf('{') >= 0) {
      return MessageFormat.format(value, params);
    }

    return value;
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
