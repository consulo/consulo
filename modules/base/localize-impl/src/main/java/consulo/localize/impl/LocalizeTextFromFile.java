/*
 * Copyright 2013-2024 consulo.io
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
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-30
 */
public class LocalizeTextFromFile extends LocalizeLoader<String> implements Supplier<String> {
    public LocalizeTextFromFile(String localizeId, PluginDescriptor pluginDescriptor, String resourcePath) {
        super(localizeId, pluginDescriptor, resourcePath);
    }

    @Override
    public String get() {
        return getValue();
    }

    @Override
    protected String getInvalidValue() {
        return "ERROR. Failed load: " + myResourcePath;
    }

    @Override
    protected String load(InputStream stream) throws IOException {
        byte[] bytes = StreamUtil.loadFromStream(stream);
        return StringUtil.convertLineSeparators(new String(bytes, StandardCharsets.UTF_8));
    }
}
