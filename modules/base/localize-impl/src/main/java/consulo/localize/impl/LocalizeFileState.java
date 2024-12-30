/*
 * Copyright 2013-2020 consulo.io
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

import consulo.container.plugin.PluginDescriptor;
import consulo.localize.LocalizeKey;
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
class LocalizeFileState extends LocalizeLoader<Map<String, LocalizeKeyText>> {
    private static final Logger LOG = Logger.getInstance(LocalizeFileState.class);

    private Map<String, LocalizeTextFromFile> myTextFromFiles;

    public LocalizeFileState(String localizeId, PluginDescriptor pluginDescriptor, String resourcePath) {
        super(localizeId, pluginDescriptor, resourcePath);
    }

    public void putTextFromFile(String localizeId, LocalizeTextFromFile textFromFile) {
        if (myTextFromFiles == null) {
            myTextFromFiles = new HashMap<>();
        }

        myTextFromFiles.putIfAbsent(localizeId, textFromFile);
    }

    @Nullable
    public String getValue(LocalizeKey key) {
        if (myTextFromFiles != null) {
            LocalizeTextFromFile text = myTextFromFiles.get(key.getKey());
            if (text != null) {
                return text.get();
            }
        }

        LocalizeKeyText text = getValue().get(key.getKey());
        return text == null ? null : text.get();
    }

    @Override
    protected Map<String, LocalizeKeyText> getInvalidValue() {
        return Map.of();
    }

    @Override
    protected Map<String, LocalizeKeyText> load() {
        long time = System.currentTimeMillis();

        Map<String, LocalizeKeyText> loaded = super.load();

        if (loaded != Map.<String, LocalizeKeyText>of()) {
            LOG.info(myLocalizeId + " parsed in " + (System.currentTimeMillis() - time) + " ms. Size: " + loaded.size());
        }
        return loaded;
    }

    @Override
    protected Map<String, LocalizeKeyText> load(InputStream stream) throws IOException {
        Yaml yaml = new Yaml();
        Map<String, Map<String, String>> o = yaml.load(stream);

        Map<String, LocalizeKeyText> map = new HashMap<>();

        for (Map.Entry<String, Map<String, String>> entry : o.entrySet()) {
            String key = entry.getKey();
            Map<String, String> value = entry.getValue();

            LocalizeKeyText instance = new LocalizeKeyText(StringUtil.notNullize(value.get("text")));

            map.put(key.toLowerCase(Locale.ROOT), instance);
        }

        return map;
    }
}
