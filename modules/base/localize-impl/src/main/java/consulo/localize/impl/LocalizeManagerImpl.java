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

import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeManager;
import consulo.localize.LocalizeManagerListener;
import consulo.localize.LocalizeValue;
import consulo.localize.internal.LocalizeManagerEx;
import consulo.logging.Logger;
import consulo.proxy.EventDispatcher;
import consulo.util.io.URLUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public class LocalizeManagerImpl extends LocalizeManager implements LocalizeManagerEx {
    private record PluginFileInfo(PluginDescriptor descriptor, String path) {
    }

    private static final Logger LOG = Logger.getInstance(LocalizeManagerImpl.class);

    private static final Locale ourDefaultLocale = Locale.US;

    private final AtomicBoolean myInitialized = new AtomicBoolean();

    public static final String LOCALIZE_DIRECTORY = "LOCALIZE-LIB/";

    private final Map<Locale, Map<String, LocalizeFileState>> myLocalizes = new HashMap<>();

    private Locale myCurrentLocale = ourDefaultLocale;

    private final EventDispatcher<LocalizeManagerListener> myEventDispatcher = EventDispatcher.create(LocalizeManagerListener.class);

    private final AtomicLong myModificationCount = new AtomicLong();

    @Override
    public void initialize() {
        if (myInitialized.compareAndSet(false, true)) {
            try {
                init();
            }
            catch (Exception e) {
                LOG.error("Fail to initialize", e);
            }

            myModificationCount.incrementAndGet();
        }
    }

    private void init() {
        List<PluginFileInfo> forLoad = new ArrayList<>();

        PluginManager.forEachEnabledPlugin(descriptor -> {
            ClassLoader classLoader = descriptor.getPluginClassLoader();

            if (!(classLoader instanceof PluginClassLoader pluginClassLoader)) {
                return;
            }

            Map<URL, Set<String>> urlsIndex = pluginClassLoader.getUrlsIndex();
            if (urlsIndex != null) {
                for (Set<String> filePaths : urlsIndex.values()) {
                    for (String filePath : filePaths) {
                        if (filePath.startsWith(LOCALIZE_DIRECTORY) && filePath.endsWith(".yaml")) {
                            forLoad.add(new PluginFileInfo(descriptor, filePath));
                        }
                    }
                }
            }
            else {
                legacySearch(descriptor, forLoad);
            }
        });

        load(forLoad);
    }

    private void legacySearch(PluginDescriptor pluginDescriptor,
                              List<PluginFileInfo> forLoad) {
        PluginClassLoader pluginClassLoader = (PluginClassLoader) pluginDescriptor.getPluginClassLoader();

        try {
            Enumeration<URL> ownResources = pluginClassLoader.findOwnResources(LOCALIZE_DIRECTORY);

            while (ownResources.hasMoreElements()) {
                URL url = ownResources.nextElement();

                Pair<String, String> urlFileInfo = URLUtil.splitJarUrl(url.getFile());
                if (urlFileInfo == null) {
                    continue;
                }

                String path = urlFileInfo.getSecond();
                if (path.endsWith(".yaml")) {
                    forLoad.add(new PluginFileInfo(pluginDescriptor, path));
                }
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    public void load(@Nonnull List<PluginFileInfo> files) {
        for (PluginFileInfo fileInfo : files) {
            try {
                load(fileInfo);
            }
            catch (Exception e) {
                LOG.error("Fail to analyze library from url: " + fileInfo, e);
            }
        }
    }

    private void load(PluginFileInfo fileInfo) throws IOException {
        String zipEntryName = fileInfo.path();

        String fullFilePath = zipEntryName.substring(LOCALIZE_DIRECTORY.length(), zipEntryName.length());

        String localeStr = fullFilePath.substring(0, fullFilePath.indexOf('/'));

        Locale locale = buildLocale(localeStr);

        // -5 - its '.yaml' prefix
        String localizeId = fullFilePath.substring(localeStr.length() + 1, fullFilePath.length() - 5);

        Map<String, LocalizeFileState> mapByLocalizeId = myLocalizes.computeIfAbsent(locale, l -> new HashMap<>());

        mapByLocalizeId.put(localizeId, new LocalizeFileState(localizeId, fileInfo.descriptor(), zipEntryName));
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

        if (fireEvents) {
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
        if (values.size() != 2) {
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
