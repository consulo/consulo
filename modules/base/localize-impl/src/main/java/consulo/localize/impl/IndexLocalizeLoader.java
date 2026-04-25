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
package consulo.localize.impl;

import consulo.container.plugin.PluginDescriptor;
import consulo.localize.LocalizeKey;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-04-25
 */
public class IndexLocalizeLoader extends LocalizeLoader<Map<String, LocalizeKeyText>> {
    private final Map<String, LocalizeKeyText> myData;

    public IndexLocalizeLoader(String localizeId,
                               PluginDescriptor pluginDescriptor,
                               Map<String, LocalizeKeyText> data) {
        super(localizeId, pluginDescriptor, "");
        myData = data;
    }

    @Override
    protected Map<String, LocalizeKeyText> load() {
        return myData;
    }

    @Override
    public @Nullable String getValue(LocalizeKey key) {
        LocalizeKeyText text = myData.get(key.getKey());
        return text == null ? null : text.text();
    }

    @Override
    protected Map<String, LocalizeKeyText> getInvalidValue() {
        throw new UnsupportedOperationException("Must be never called, due it preloaded");
    }

    @Override
    protected Map<String, LocalizeKeyText> load(InputStream stream) throws IOException {
        throw new UnsupportedOperationException("Must be never called, due it preloaded");
    }
}
