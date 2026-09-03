/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal.configurable;

import consulo.configurable.ConfigurationException;
import consulo.configurable.Settings;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.Comparing;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.localize.VcsLocalize;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Kirill Likhodedov
 */
class VcsUpdateInfoScopeFilterConfigurable implements NamedScopesHolder.ScopeListener {
    private final Project myProject;
    private final VcsConfiguration myVcsConfiguration;
    private final NamedScopesHolder[] myNamedScopeHolders;

    private final MutableFlatDataModel<String> myScopeModel = FlatDataModel.of(List.of());

    private @Nullable CheckBox myCheckBox;
    private @Nullable ComboBox<String> myComboBox;

    VcsUpdateInfoScopeFilterConfigurable(Project project, VcsConfiguration vcsConfiguration) {
        myProject = project;
        myVcsConfiguration = vcsConfiguration;

        myNamedScopeHolders = NamedScopesHolder.getAllNamedScopeHolders(myProject);
        for (NamedScopesHolder holder : myNamedScopeHolders) {
            holder.addScopeListener(this);
        }
    }

    @Override
    public void scopesChanged() {
        reset();
    }

    @RequiredUIAccess
    public Component createComponent(Disposable uiDisposable) {
        CheckBox checkBox = CheckBox.create(VcsLocalize.settingsFilterUpdateProjectInfoByScope());
        ComboBox<String> comboBox = ComboBox.create(myScopeModel);

        comboBox.setEnabled(checkBox.getValueOrError());
        checkBox.addValueListener(event -> comboBox.setEnabled(event.getValue()));

        myCheckBox = checkBox;
        myComboBox = comboBox;

        Hyperlink editScopes = Hyperlink.create(VcsLocalize.settingsEditScopes(), event -> {
            Settings settings = DataManager.getInstance().getDataContext(comboBox).getData(Settings.KEY);
            if (settings != null) {
                settings.select("project.scopes");
            }
        });

        return HorizontalLayout.create().add(checkBox).add(comboBox).add(editScopes);
    }

    @RequiredUIAccess
    public boolean isModified() {
        return !Comparing.equal(myVcsConfiguration.UPDATE_FILTER_SCOPE_NAME, getScopeFilterName());
    }

    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        myVcsConfiguration.UPDATE_FILTER_SCOPE_NAME = getScopeFilterName();
    }

    @RequiredUIAccess
    public void reset() {
        if (myCheckBox == null || myComboBox == null) {
            return;
        }

        List<String> scopes = new ArrayList<>();
        for (NamedScopesHolder holder : myNamedScopeHolders) {
            for (NamedScope scope : holder.getEditableScopes()) {
                scopes.add(scope.getName());
            }
        }
        myScopeModel.replaceAll(scopes);

        boolean selection = scopes.contains(myVcsConfiguration.UPDATE_FILTER_SCOPE_NAME);
        if (selection) {
            myComboBox.setValue(myVcsConfiguration.UPDATE_FILTER_SCOPE_NAME);
        }
        myCheckBox.setValue(selection);
        myComboBox.setEnabled(selection);
    }

    @RequiredUIAccess
    public void disposeUIResources() {
        for (NamedScopesHolder holder : myNamedScopeHolders) {
            holder.removeScopeListener(this);
        }
        myCheckBox = null;
        myComboBox = null;
    }

    @RequiredUIAccess
    private @Nullable String getScopeFilterName() {
        if (myCheckBox == null || myComboBox == null || !myCheckBox.getValueOrError()) {
            return null;
        }
        return myComboBox.getValue();
    }
}
