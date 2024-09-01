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
import consulo.container.plugin.PluginDescriptorStatus;
import consulo.localize.LocalizeKey;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-05-20
 */
class LocalizeFileState {
    private static final Logger LOG = Logger.getInstance(LocalizeFileState.class);

    private final String myLocalizeId;
    private final PluginDescriptor myPluginDescriptor;
    private final String myResourcePath;

    private volatile Map<String, LocalizeKeyText> myTexts;

    public LocalizeFileState(String localizeId, PluginDescriptor pluginDescriptor, String resourcePath) {
        myLocalizeId = localizeId;
        myPluginDescriptor = pluginDescriptor;
        myResourcePath = resourcePath;
    }

    @Nullable
    public String getValue(LocalizeKey key) {
        Map<String, LocalizeKeyText> texts = myTexts;

        if (texts == null) {
            texts = loadTexts();
            myTexts = texts;
        }

        LocalizeKeyText text = texts.get(key.getKey());
        return text == null ? null : text.getText();
    }

    @Nonnull
    private Map<String, LocalizeKeyText> loadTexts() {
        Map<String, LocalizeKeyText> map = new HashMap<>();

        long time = System.currentTimeMillis();

        // descritor
        if (myPluginDescriptor.getStatus() != PluginDescriptorStatus.OK) {
            LOG.info(myLocalizeId + " plugin status is not ok: " + myPluginDescriptor.getPluginId());
            return Map.of();
        }

        InputStream inputStream = myPluginDescriptor.getPluginClassLoader().getResourceAsStream(myResourcePath);
        if (inputStream == null) {
            LOG.info(myLocalizeId + " can't find " + myResourcePath);
            return Map.of();
        }

        Yaml yaml = new Yaml();
        try (inputStream) {
            Map<String, Map<String, String>> o = yaml.load(inputStream);

            for (Map.Entry<String, Map<String, String>> entry : o.entrySet()) {
                String key = entry.getKey();
                Map<String, String> value = entry.getValue();

                LocalizeKeyText instance = new LocalizeKeyText(StringUtil.notNullize(value.get("text")));

                map.put(key.toLowerCase(Locale.ROOT), instance);
            }
        }
        catch (Exception e) {
            LOG.error(e);
        }

        LOG.info(myLocalizeId + " parsed in " + (System.currentTimeMillis() - time) + " ms. Size: " + map.size());
        return map;
    }
}
