/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.configurable;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;

@Deprecated
@DeprecationInfo("Use SimpleConfigurable")
public abstract class IdeaConfigurableBase<UI extends IdeaConfigurableUi<S>, S> implements SearchableConfigurable {
    private final String id;
    private final String displayName;
    private final String helpTopic;

    private UI ui;

    protected IdeaConfigurableBase(@Nonnull String id, @Nonnull String displayName, @Nullable String helpTopic) {
        this.id = id;
        this.displayName = displayName;
        this.helpTopic = helpTopic;
    }

    @Nonnull
    @Override
    public final String getId() {
        return id;
    }

    @Nls
    @Override
    public final String getDisplayName() {
        return displayName;
    }

    @Nullable
    @Override
    public final String getHelpTopic() {
        return helpTopic;
    }

    @Nullable
    @Override
    public Runnable enableSearch(String option) {
        return null;
    }

    @Nonnull
    protected abstract S getSettings();

    @Override
    public void reset() {
        if (ui != null) {
            ui.reset(getSettings());
        }
    }

    @Nullable
    @Override
    public final JComponent createComponent(Disposable disposable) {
        if (ui == null) {
            ui = createUi();
        }
        return ui.getComponent(disposable);
    }

    protected abstract UI createUi();

    @Override
    public final boolean isModified() {
        return ui != null && ui.isModified(getSettings());
    }

    @Override
    public final void apply() throws ConfigurationException {
        if (ui != null) {
            ui.apply(getSettings());
        }
    }

    @Override
    public void disposeUIResources() {
        ui = null;
    }
}