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
package consulo.container.classloader;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2019-11-23
 */
public interface PluginClassLoader {
    PluginId getPluginId();

    PluginDescriptor getPluginDescriptor();

    boolean hasLoadedClass(String className);

    Enumeration<URL> findOwnResources(String name) throws IOException;

    Map<URL, Set<String>> getUrlsIndex();
}
