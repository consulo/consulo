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

import com.ibm.icu.text.MessageFormat;
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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public class LocalizeManagerImpl extends LocalizeManager implements LocalizeManagerEx {
    private record PluginFileInfo(String id, PluginDescriptor descriptor, String path, Set<String> files) {
    }

    private static final Logger LOG = Logger.getInstance(LocalizeManagerImpl.class);

    private static final Locale ourDefaultLocale = Locale.US;

    private final AtomicBoolean myInitialized = new AtomicBoolean();

    public static final String LOCALIZE_DIRECTORY = "LOCALIZE-LIB/";

    private static final String YAML_EXTENSION = ".yaml";

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
                    Map<String, PluginFileInfo> loadInfo = new HashMap<>();

                    for (String filePath : filePaths) {
                        if (filePath.startsWith(LOCALIZE_DIRECTORY) && filePath.endsWith(YAML_EXTENSION)) {
                            String pathId = filePath.substring(0, filePath.length() - YAML_EXTENSION.length());

                            loadInfo.put(pathId, new PluginFileInfo(pathId, descriptor, filePath, new HashSet<>()));
                        }
                    }

                    for (String filePath : filePaths) {
                        if (filePath.startsWith(LOCALIZE_DIRECTORY) && !filePath.endsWith(YAML_EXTENSION)) {
                            int index = getIndexOf(filePath, '/', 3);
                            if (index == -1) {
                                LOG.warn("Invalid localize path: " + filePath);
                                continue;
                            }

                            String yamlIdPath = filePath.substring(0, index);
                            PluginFileInfo info = loadInfo.get(yamlIdPath);
                            if (info == null) {
                                LOG.warn("Localize yaml not loaded. Path: " + yamlIdPath);
                                continue;
                            }

                            info.files().add(filePath);
                        }
                    }

                    forLoad.addAll(loadInfo.values());
                }
            }
            else {
                legacySearch(descriptor, forLoad);
            }
        });

        load(forLoad);
    }

    private int getIndexOf(String str, char symbol, int atCount) {
        int visited = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == symbol) {
                visited ++;

                if (visited == atCount) {
                    return i;
                }
            }
        }

        return -1;
    }

    private void legacySearch(PluginDescriptor pluginDescriptor, List<PluginFileInfo> forLoad) {
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
                if (path.endsWith(YAML_EXTENSION)) {
                    String yamlId = path.substring(0, path.length() - YAML_EXTENSION.length());
                    forLoad.add(new PluginFileInfo(yamlId, pluginDescriptor, path, Set.of()));
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
        String localizeId = fullFilePath.substring(localeStr.length() + 1, fullFilePath.length() - YAML_EXTENSION.length());

        Map<String, LocalizeFileState> mapByLocalizeId = myLocalizes.computeIfAbsent(locale, l -> new HashMap<>());

        LocalizeFileState state = new LocalizeFileState(localizeId, fileInfo.descriptor(), zipEntryName);

        mapByLocalizeId.put(localizeId, state);

        if (!fileInfo.files().isEmpty()) {
            for (String subFile : fileInfo.files()) {
                // zipEntryName = "LOCALIZE-LIB/en_US/consulo.language.LanguageLocalize.yaml"
                // file = "LOCALIZE-LIB/en_US/consulo.language.LanguageLocalize/inspection/SyntaxError.html"
                // fileId = "inspection/SyntaxError.html"
                String fileId = subFile.substring(zipEntryName.length() - YAML_EXTENSION.length() + 1, subFile.length());

                int lastDotIndex = fileId.lastIndexOf('.');
                if (lastDotIndex != -1) {
                    fileId = fileId.substring(0, lastDotIndex);
                }

                String fileLocalizeId = fileId.replace('/', '.').toLowerCase(Locale.ROOT);

                state.putTextFromFile(fileLocalizeId, new LocalizeTextFromFile(localizeId, fileInfo.descriptor(), subFile));
            }
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
    public Map.Entry<Locale, String> getUnformattedText(@Nonnull LocalizeKey key) {
        if (!myInitialized.get()) {
            throw new IllegalArgumentException("not initialized");
        }

        if (StringUtil.isEmptyOrSpaces(key.getKey())) {
            return Map.entry(myCurrentLocale, "");
        }

        String value = getValue(key, myCurrentLocale);
        if (value != null) {
            return Map.entry(myCurrentLocale, value);
        }

        value = getValue(key, ourDefaultLocale);
        if (value != null) {
            return Map.entry(ourDefaultLocale, value);
        }

        LOG.warn("Can't find localize value: " + key + ", current locale: " + myCurrentLocale);
        return Map.entry(ourDefaultLocale, key.toString());
    }

    @Nonnull
    @Override
    public String formatText(String unformattedText, Locale locale, Object... args) {
        MessageFormat format = new MessageFormat(unformattedText, locale);
        return format.format(args);
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
