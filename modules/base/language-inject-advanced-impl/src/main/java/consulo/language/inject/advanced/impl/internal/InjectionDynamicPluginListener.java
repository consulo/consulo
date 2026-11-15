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
package consulo.language.inject.advanced.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.application.Application;
import consulo.application.plugin.DynamicPluginChange;
import consulo.application.plugin.DynamicPluginListener;
import consulo.language.inject.advanced.impl.internal.pattern.PatternCompilerFactoryImpl;
import consulo.language.inject.advanced.internal.ApplicationInjectionConfiguration;
import consulo.language.inject.advanced.pattern.PatternCompilerFactory;
import jakarta.inject.Inject;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

/**
 * Injections and pattern classes follow plugins loaded or unloaded at runtime.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
@TopicImpl(ComponentScope.APPLICATION)
public class InjectionDynamicPluginListener implements DynamicPluginListener {
    private final Application myApplication;
    private @Nullable Element myInjectionState;

    @Inject
    public InjectionDynamicPluginListener(Application application) {
        myApplication = application;
    }

    @Override
    public void beforePluginsChange(DynamicPluginChange change) {
        // written while the injection supports of the unloading plugins still exist
        myInjectionState = myApplication.getInstance(ApplicationInjectionConfiguration.class).getState();
    }

    @Override
    public void pluginsChanged(DynamicPluginChange change) {
        Element state = myInjectionState;
        myInjectionState = null;
        if (state != null) {
            myApplication.getInstance(ApplicationInjectionConfiguration.class).reloadState(state);
        }

        if (PatternCompilerFactory.getFactory() instanceof PatternCompilerFactoryImpl factory) {
            factory.dropCachesIfProvidersChanged();
        }
    }
}
