/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.execution.internal;

import consulo.application.Application;
import consulo.component.extension.ExtensionException;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.action.*;
import consulo.execution.configuration.ConfigurationType;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PreferredProducerFind {
    private static final Logger LOG = Logger.getInstance(PreferredProducerFind.class);

    private PreferredProducerFind() {
    }

    @Nullable
    public static RunnerAndConfigurationSettings createConfiguration(@Nonnull Location location, ConfigurationContext context) {
        ConfigurationFromContext fromContext = findConfigurationFromContext(location, context);
        return fromContext != null ? fromContext.getConfigurationSettings() : null;
    }

    @Nullable
    @Deprecated
    @SuppressWarnings("deprecation")
    public static List<RuntimeConfigurationProducer> findPreferredProducers(
        Location location,
        ConfigurationContext context,
        boolean strict
    ) {
        if (location == null) {
            return null;
        }
        List<RuntimeConfigurationProducer> producers = findAllProducers(location, context);
        if (producers.isEmpty()) {
            return null;
        }
        Collections.sort(producers, RuntimeConfigurationProducer.COMPARATOR);

        if (strict) {
            RuntimeConfigurationProducer first = producers.get(0);
            for (Iterator<RuntimeConfigurationProducer> it = producers.iterator(); it.hasNext(); ) {
                RuntimeConfigurationProducer producer = it.next();
                if (producer != first && RuntimeConfigurationProducer.COMPARATOR.compare(producer, first) >= 0) {
                    it.remove();
                }
            }
        }

        return producers;
    }

    @Deprecated
    @SuppressWarnings("deprecation")
    private static List<RuntimeConfigurationProducer> findAllProducers(Location location, ConfigurationContext context) {
        //todo load configuration types if not already loaded
        Application.get().getExtensionList(ConfigurationType.class);
        List<RuntimeConfigurationProducer> configurationProducers =
            RuntimeConfigurationProducer.RUNTIME_CONFIGURATION_PRODUCER.getExtensionList();
        ArrayList<RuntimeConfigurationProducer> producers = new ArrayList<>();
        for (RuntimeConfigurationProducer prototype : configurationProducers) {
            RuntimeConfigurationProducer producer;
            try {
                producer = prototype.createProducer(location, context);
            }
            catch (AbstractMethodError e) {
                LOG.error(new ExtensionException(prototype.getClass()));
                continue;
            }
            if (producer.getConfiguration() != null) {
                LOG.assertTrue(producer.getSourceElement() != null, producer);
                producers.add(producer);
            }
        }
        return producers;
    }

    public static List<ConfigurationFromContext> getConfigurationsFromContext(
        Location location,
        ConfigurationContext context,
        boolean strict
    ) {
        return getConfigurationsFromContext(location, context, strict, true);

    }

    public static List<ConfigurationFromContext> getConfigurationsFromContext(
        Location location,
        ConfigurationContext context,
        boolean strict,
        boolean preferExisting
    ) {
        if (location == null) {
            return null;
        }

        ArrayList<ConfigurationFromContext> configurationsFromContext = new ArrayList<>();
        for (RuntimeConfigurationProducer producer : findAllProducers(location, context)) {
            configurationsFromContext.add(new ConfigurationFromContextWrapper(producer));
        }

        Application.get().getExtensionPoint(RunConfigurationProducer.class).collectExtensionsSafe(
            configurationsFromContext,
            producer -> producer.findOrCreateConfigurationFromContext(context, preferExisting)
        );

        if (configurationsFromContext.isEmpty()) {
            return null;
        }
        Collections.sort(configurationsFromContext, ConfigurationFromContext.COMPARATOR);

        if (strict) {
            ConfigurationFromContext first = configurationsFromContext.get(0);
            for (Iterator<ConfigurationFromContext> it = configurationsFromContext.iterator(); it.hasNext(); ) {
                ConfigurationFromContext producer = it.next();
                if (producer != first && ConfigurationFromContext.COMPARATOR.compare(producer, first) > 0) {
                    it.remove();
                }
            }
        }

        return configurationsFromContext;
    }

    @Nullable
    private static ConfigurationFromContext findConfigurationFromContext(Location location, ConfigurationContext context) {
        List<ConfigurationFromContext> producers = getConfigurationsFromContext(location, context, true);
        return producers != null ? producers.get(0) : null;
    }
}
