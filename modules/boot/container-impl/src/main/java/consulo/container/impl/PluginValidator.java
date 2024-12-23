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
package consulo.container.impl;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginDescriptorVersionValidator;
import consulo.container.plugin.PluginId;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2020-05-23
 */
public class PluginValidator {

    public static PluginDescriptorVersionValidator VALIDATOR = new PluginDescriptorVersionValidator() {
        @Override
        public boolean validateVersion(PluginDescriptor pluginDescriptor) {
            return true;
        }
    };

    public static boolean isIncompatible(final PluginDescriptor descriptor) {
        return !VALIDATOR.validateVersion(descriptor);
    }

    public static void checkDependants(final PluginDescriptor pluginDescriptor, final Function<PluginId, PluginDescriptor> pluginId2Descriptor, final Predicate<PluginId> check) {
        checkDependants(pluginDescriptor, pluginId2Descriptor, check, new HashSet<>());
    }

    private static boolean checkDependants(final PluginDescriptor pluginDescriptor,
                                           final Function<PluginId, PluginDescriptor> pluginId2Descriptor,
                                           final Predicate<PluginId> check,
                                           final Set<PluginId> processed) {
        processed.add(pluginDescriptor.getPluginId());
        final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
        final Set<PluginId> optionalDependencies = new HashSet<PluginId>(Arrays.asList(pluginDescriptor.getOptionalDependentPluginIds()));
        for (final PluginId dependentPluginId : dependentPluginIds) {
            if (processed.contains(dependentPluginId)) {
                continue;
            }
            if (!optionalDependencies.contains(dependentPluginId)) {
                if (!check.test(dependentPluginId)) {
                    return false;
                }
                final PluginDescriptor dependantPluginDescriptor = pluginId2Descriptor.apply(dependentPluginId);
                if (dependantPluginDescriptor != null && !checkDependants(dependantPluginDescriptor, pluginId2Descriptor, check, processed)) {
                    return false;
                }
            }
        }
        return true;
    }
}
