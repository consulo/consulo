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
package consulo.ide.impl.ui.app.impl.settings;

import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.configurable.Settings;
import consulo.configurable.UnnamedConfigurable;
import consulo.configurable.internal.ConfigurableWrapper;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class OptionsEditorSettings implements Settings {
    private final OptionsEditorHost myHost;

    public OptionsEditorSettings(OptionsEditorHost host) {
        myHost = host;
    }

    @Override
    public @Nullable <T extends Configurable> T findConfigurable(Class<T> configurableClass) {
        for (Configurable configurable : myHost.getTreeConfigurables()) {
            T cast = ConfigurableWrapper.cast(configurable, configurableClass);
            if (cast != null) {
                return cast;
            }
        }
        return null;
    }

    @Override
    public @Nullable SearchableConfigurable findConfigurableById(String configurableId) {
        for (Configurable configurable : myHost.getTreeConfigurables()) {
            if (configurable instanceof SearchableConfigurable searchableConfigurable
                && configurableId.equals(searchableConfigurable.getId())) {
                return searchableConfigurable;
            }
        }
        return null;
    }

    private @Nullable Configurable findConfigurableOwner(Class<? extends UnnamedConfigurable> configurableClass) {
        for (Configurable configurable : myHost.getTreeConfigurables()) {
            if (ConfigurableWrapper.cast(configurable, configurableClass) != null) {
                return configurable;
            }
        }
        return null;
    }

    @Override
    public CompletableFuture<Void> clearSearchAndSelect(Configurable configurable) {
        myHost.clearFilter();
        return select(configurable, "");
    }

    @Override
    public CompletableFuture<Void> select(Configurable configurable) {
        String filterText = myHost.getFilterText();
        if (StringUtil.isEmpty(filterText)) {
            return select(configurable, "");
        }
        else {
            return myHost.refilter(filterText, true, true).thenApply(ignored -> null);
        }
    }

    @Override
    public CompletableFuture<Void> select(Configurable configurable, String text) {
        return refilterAndSelect(text, configurable).thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Configurable> select(String configurableId) {
        Configurable configurable = findConfigurableById(configurableId);
        if (configurable == null) {
            return CompletableFuture.failedFuture(new CancellationException());
        }

        return refilterAndSelect("", configurable).thenApply(ignored -> configurable);
    }

    @Override
    public <T extends UnnamedConfigurable> CompletableFuture<T> select(Class<T> clazz) {
        Configurable configurable = findConfigurableOwner(clazz);
        if (configurable == null) {
            return CompletableFuture.failedFuture(new CancellationException());
        }

        T unwrapped = ConfigurableWrapper.cast(configurable, clazz);
        return refilterAndSelect("", configurable).thenApply(ignored -> unwrapped);
    }

    private CompletableFuture<?> refilterAndSelect(String text, Configurable configurable) {
        return myHost.refilter(text, false, true).thenCompose(ignored -> myHost.selectInTree(configurable));
    }
}
