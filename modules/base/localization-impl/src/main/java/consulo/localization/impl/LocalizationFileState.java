/*
 * Copyright 2013-2026 consulo.io
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
package consulo.localization.impl;

import consulo.container.plugin.PluginDescriptor;
import consulo.localization.LocalizationKey;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-05-20
 */
class LocalizationFileState extends LocalizationLoader<Map<String, LocalizationKeyText>> {
    private static final Logger LOG = Logger.getInstance(LocalizationFileState.class);

    private Map<String, LocalizationTextFromFile> myTextFromFiles;

    public LocalizationFileState(String localizeId, PluginDescriptor pluginDescriptor, String resourcePath) {
        super(localizeId, pluginDescriptor, resourcePath);
    }

    public void putTextFromFile(String localizeId, LocalizationTextFromFile textFromFile) {
        if (myTextFromFiles == null) {
            myTextFromFiles = new HashMap<>();
        }

        myTextFromFiles.putIfAbsent(localizeId, textFromFile);
    }

    @Nullable
    public String getValue(LocalizationKey key) {
        if (myTextFromFiles != null) {
            LocalizationTextFromFile text = myTextFromFiles.get(key.getKey());
            if (text != null) {
                return text.get();
            }
        }

        LocalizationKeyText text = getValue().get(key.getKey());
        return text == null ? null : text.get();
    }

    @Override
    protected Map<String, LocalizationKeyText> getInvalidValue() {
        return Map.of();
    }

    @Override
    protected Map<String, LocalizationKeyText> load() {
        long time = System.currentTimeMillis();

        Map<String, LocalizationKeyText> loaded = super.load();

        if (loaded != Map.<String, LocalizationKeyText>of()) {
            LOG.info(myLocalizeId + " parsed in " + (System.currentTimeMillis() - time) + " ms. Size: " + loaded.size());
        }
        return loaded;
    }

    @Override
    protected Map<String, LocalizationKeyText> load(InputStream stream) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Map<String, String>> o = yaml.load(stream);

        Map<String, LocalizationKeyText> map = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : o.entrySet()) {
            String key = entry.getKey();
            Map<String, String> value = entry.getValue();

            LocalizationKeyText instance = new LocalizationKeyText(StringUtil.notNullize(value.get("text")));

            map.put(key.toLowerCase(Locale.ROOT), instance);
        }

        return map;
    }
}
