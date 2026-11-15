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
package consulo.application.impl.internal.plugin;

import consulo.application.plugin.DynamicPluginChange;
import consulo.container.plugin.PluginDescriptor;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
public class DynamicPluginChangeImpl implements DynamicPluginChange {
    private final List<PluginDescriptor> myUnloading;
    private final List<PluginDescriptor> myLoading;

    public DynamicPluginChangeImpl(List<PluginDescriptor> unloading, List<PluginDescriptor> loading) {
        myUnloading = List.copyOf(unloading);
        myLoading = List.copyOf(loading);
    }

    @Override
    public List<PluginDescriptor> getUnloading() {
        return myUnloading;
    }

    @Override
    public List<PluginDescriptor> getLoading() {
        return myLoading;
    }
}
