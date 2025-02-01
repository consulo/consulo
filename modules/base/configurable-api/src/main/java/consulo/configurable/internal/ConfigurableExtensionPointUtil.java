/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.configurable.internal;

import consulo.configurable.Configurable;
import consulo.configurable.OptionalConfigurable;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * @author nik
 */
public class ConfigurableExtensionPointUtil {
    private final static Logger LOG = Logger.getInstance(ConfigurableExtensionPointUtil.class);

    private ConfigurableExtensionPointUtil() {
    }

    public static List<Configurable> buildConfigurablesList(List<Configurable> extensions, @Nullable Predicate<Configurable> filter) {
        final List<Configurable> result = new ArrayList<>();

        final Map<String, ConfigurableWrapper> idToConfigurable = new HashMap<>();
        for (Configurable configurable : extensions) {
            // do not disable if disable
            if (configurable instanceof OptionalConfigurable && !((OptionalConfigurable) configurable).needDisplay()) {
                continue;
            }

            if (filter == null || !filter.test(configurable)) {
                continue;
            }

            idToConfigurable.put(configurable.getId(), ConfigurableWrapper.wrapConfigurable(configurable));
        }

        //modify configurables (append children)
        for (final String id : idToConfigurable.keySet()) {
            final ConfigurableWrapper wrapper = idToConfigurable.get(id);
            final String parentId = wrapper.getParentId();
            if (parentId != null) {
                final ConfigurableWrapper parent = idToConfigurable.get(parentId);
                if (parent != null) {
                    idToConfigurable.put(parentId, parent.addChild(wrapper));
                }
                else {
                    LOG.error("Can't find parent for " + parentId + " (" + wrapper + ")");
                }
            }
        }
        //leave only roots (i.e. configurables without parents)
        for (final Iterator<String> iterator = idToConfigurable.keySet().iterator(); iterator.hasNext(); ) {
            final String key = iterator.next();
            final ConfigurableWrapper wrapper = idToConfigurable.get(key);
            if (wrapper.getParentId() != null) {
                iterator.remove();
            }
        }
        ContainerUtil.addAll(result, idToConfigurable.values());

        return result;
    }
}
