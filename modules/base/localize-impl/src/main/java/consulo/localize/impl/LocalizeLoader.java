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
import consulo.container.plugin.PluginDescriptorStatus;
import consulo.logging.Logger;
import consulo.util.lang.lazy.LazyValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2024-12-30
 */
public abstract class LocalizeLoader<V> {
    private static final Logger LOG = Logger.getInstance(LocalizeLoader.class);

    protected final String myLocalizeId;
    protected final PluginDescriptor myPluginDescriptor;
    protected final String myResourcePath;

    private volatile Supplier<V> myValue;

    public LocalizeLoader(String localizeId, PluginDescriptor pluginDescriptor, String resourcePath) {
        myLocalizeId = localizeId;
        myPluginDescriptor = pluginDescriptor;
        myResourcePath = resourcePath;

        myValue = LazyValue.notNull(this::load);
    }

    protected V getValue() {
        return myValue.get();
    }

    protected abstract V getInvalidValue();

    protected abstract V load(InputStream stream) throws IOException;

    protected V load() {
        if (myPluginDescriptor.getStatus() != PluginDescriptorStatus.OK) {
            LOG.info(myLocalizeId + " plugin status is not ok: " + myPluginDescriptor.getPluginId());
            return getInvalidValue();
        }

        InputStream inputStream = myPluginDescriptor.getPluginClassLoader().getResourceAsStream(myResourcePath);
        if (inputStream == null) {
            LOG.info(myLocalizeId + " can't find " + myResourcePath);
            return getInvalidValue();
        }

        try (inputStream) {
            return load(inputStream);
        }
        catch (Exception e) {
            LOG.error(e);
        }
        return getInvalidValue();
    }
}
