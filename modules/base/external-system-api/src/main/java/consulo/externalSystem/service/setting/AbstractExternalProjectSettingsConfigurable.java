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
package consulo.externalSystem.service.setting;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.externalSystem.localize.ExternalSystemLocalize;
import consulo.externalSystem.setting.ExternalProjectSettings;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

/**
 * Base for configurables which edit the settings of a single external project. Single ide project might contain multiple bindings to
 * external projects, e.g. one module is backed by a single external project and couple of others are backed by a single external
 * multi-project.
 *
 * @author Denis Zhdanov
 * @author VISTALL
 * @since 2013-04-24
 */
public abstract class AbstractExternalProjectSettingsConfigurable<S extends ExternalProjectSettings>
    extends ExternalSystemSettingsConfigurable<S> {

    private final ExternalSystemSettingsPlace myPlace;

    private @Nullable CheckBox myCreateEmptyContentRootDirectoriesBox;

    protected AbstractExternalProjectSettingsConfigurable(S settings, ExternalSystemSettingsPlace place) {
        super(settings);
        myPlace = place;
    }

    public ExternalSystemSettingsPlace getPlace() {
        return myPlace;
    }

    @Override
    @RequiredUIAccess
    public Component createUIComponent(Disposable uiDisposable) {
        S settings = getSettings();

        VerticalLayout layout = VerticalLayout.create();

        myCreateEmptyContentRootDirectoriesBox = CheckBox.create(
            ExternalSystemLocalize.settingsLabelCreateEmptyContentRootDirectories(),
            settings.isCreateEmptyContentRootDirectories()
        );
        layout.add(myCreateEmptyContentRootDirectoriesBox);

        Component extraComponent = createExtraUIComponent(uiDisposable);
        if (extraComponent != null) {
            layout.add(extraComponent);
        }
        return layout;
    }

    @RequiredUIAccess
    protected abstract @Nullable Component createExtraUIComponent(Disposable uiDisposable);

    /**
     * Called when the user points the import wizard at another external project.
     *
     * @param path path of the external project being imported
     */
    @RequiredUIAccess
    public void onLinkedProjectPathChange(String path) {
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        S settings = getSettings();
        if (myCreateEmptyContentRootDirectoriesBox != null
            && myCreateEmptyContentRootDirectoriesBox.getValue() != settings.isCreateEmptyContentRootDirectories()) {
            return true;
        }
        return isExtraModified();
    }

    @RequiredUIAccess
    protected abstract boolean isExtraModified();

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        S settings = getSettings();
        if (myCreateEmptyContentRootDirectoriesBox != null) {
            settings.setCreateEmptyContentRootDirectories(myCreateEmptyContentRootDirectoriesBox.getValue());
        }
        applyExtra();
    }

    @RequiredUIAccess
    protected abstract void applyExtra() throws ConfigurationException;
}
