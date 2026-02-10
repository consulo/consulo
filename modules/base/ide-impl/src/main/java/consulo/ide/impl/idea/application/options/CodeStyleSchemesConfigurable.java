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
package consulo.ide.impl.idea.application.options;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.EditorFactory;
import consulo.configurable.*;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.application.options.codeStyle.CodeStyleMainPanel;
import consulo.ide.impl.idea.application.options.codeStyle.CodeStyleSettingsPanelFactory;
import consulo.ide.impl.idea.application.options.codeStyle.NewCodeStyleSettingsPanel;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.codeStyle.impl.internal.CodeStyleSchemeImpl;
import consulo.language.codeStyle.impl.internal.CodeStyleSchemesModelImpl;
import consulo.language.codeStyle.localize.CodeStyleLocalize;
import consulo.language.codeStyle.setting.CodeStyleSettingsProvider;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ExtensionImpl
public class CodeStyleSchemesConfigurable extends SearchableConfigurable.Parent.Abstract
    implements OptionsContainingConfigurable, Configurable.NoMargin, ProjectConfigurable {

    public static final String CONFIGURABLE_ID = "preferences.sourceCode";

    private CodeStyleSchemesModelImpl myModel;
    private List<CodeStyleConfigurableWrapper> myPanels;

    private CodeStyleConfigurableWrapper myRootConfigurable;

    private boolean myResetCompleted = false;
    private boolean myInitResetInvoked = false;
    private boolean myRevertCompleted = false;

    private boolean myApplyCompleted = false;
    private final Project myProject;

    @Inject
    public CodeStyleSchemesConfigurable(Project project) {
        myProject = project;
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EDITOR_GROUP;
    }

    @Override
    @RequiredUIAccess
    public JComponent createComponent(@Nonnull Disposable uiDisposable) {
        myModel = ensureModel();

        if (myRootConfigurable != null) {
            return ConfigurableUIMigrationUtil.createComponent(myRootConfigurable, uiDisposable);
        }
        return null;
    }

    @Override
    public boolean hasOwnContent() {
        return true;
    }

    @Override
    @RequiredUIAccess
    public void disposeUIResources() {
        if (myPanels != null) {
            try {
                super.disposeUIResources();
                for (CodeStyleConfigurableWrapper panel : myPanels) {
                    panel.disposeUIResources();
                }

                if (myRootConfigurable != null) {
                    myRootConfigurable.disposeUIResources();
                }
            }
            finally {
                myPanels = null;
                myModel = null;
                myRootConfigurable = null;
                myResetCompleted = false;
                myRevertCompleted = false;
                myApplyCompleted = false;
                myInitResetInvoked = false;
            }
        }
    }

    @Override
    @RequiredUIAccess
    public synchronized void reset() {
        if (!myInitResetInvoked) {
            try {
                if (!myResetCompleted) {
                    try {
                        resetImpl();
                    }
                    finally {
                        myResetCompleted = true;
                    }
                }
            }
            finally {
                myInitResetInvoked = true;
            }
        }
        else {
            revert();
        }

    }

    private void resetImpl() {
        if (myModel != null) {
            myModel.reset();
        }

        if (myPanels != null) {
            for (CodeStyleConfigurableWrapper panel : myPanels) {
                panel.resetPanel();
            }
        }
    }

    public synchronized void resetFromChild() {
        if (!myResetCompleted) {
            try {
                resetImpl();
            }
            finally {
                myResetCompleted = true;
            }
        }
    }

    public void revert() {
        if (myModel.isSchemeListModified() || isSomeSchemeModified()) {
            myRevertCompleted = false;
        }
        if (!myRevertCompleted) {
            try {
                resetImpl();
            }
            finally {
                myRevertCompleted = true;
            }
        }
    }

    private boolean isSomeSchemeModified() {
        if (myPanels != null) {
            for (CodeStyleConfigurableWrapper panel : myPanels) {
                if (panel.isPanelModified()) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    @RequiredUIAccess
    public void apply() throws ConfigurationException {
        if (!myApplyCompleted) {
            try {
                super.apply();

                for (CodeStyleScheme scheme : new ArrayList<>(myModel.getSchemes())) {
                    boolean isDefaultModified = CodeStyleSchemesModelImpl.cannotBeModified(scheme) && isSchemeModified(scheme);
                    if (isDefaultModified) {
                        CodeStyleScheme newScheme = myModel.createNewScheme(null, scheme);
                        CodeStyleSettings settingsWillBeModified = scheme.getCodeStyleSettings();
                        CodeStyleSettings notModifiedSettings = settingsWillBeModified.clone();
                        CodeStyleSchemeImpl schemeImpl = (CodeStyleSchemeImpl) scheme;
                        schemeImpl.setCodeStyleSettings(notModifiedSettings);
                        schemeImpl.setCodeStyleSettings(settingsWillBeModified);
                        myModel.addScheme(newScheme, false);

                        if (myModel.getSelectedScheme() == scheme) {
                            myModel.selectScheme(newScheme, this);
                        }
                    }
                }

                for (CodeStyleConfigurableWrapper panel : myPanels) {
                    panel.applyPanel();
                }

                myModel.apply();
                EditorFactory.getInstance().refreshAllEditors();

                CodeStyleSettingsManager.getInstance(myProject).fireCodeStyleSettingsChanged(null);
            }
            finally {
                myApplyCompleted = true;
            }
        }
    }

    @Nullable
    public SearchableConfigurable findSubConfigurable(@Nonnull LocalizeValue name) {
        return findSubConfigurable(this, name);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    @Nullable
    public SearchableConfigurable findSubConfigurable(@Nonnull String name) {
        return findSubConfigurable(this, LocalizeValue.of(name));
    }

    private static SearchableConfigurable findSubConfigurable(SearchableConfigurable.Parent topConfigurable, @Nonnull LocalizeValue name) {
        for (Configurable configurable : topConfigurable.getConfigurables()) {
            if (configurable instanceof SearchableConfigurable searchableConfigurable) {
                if (name.equals(searchableConfigurable.getDisplayName())) {
                    return searchableConfigurable;
                }
                if (searchableConfigurable instanceof SearchableConfigurable.Parent parentConfigurable) {
                    SearchableConfigurable child = findSubConfigurable(parentConfigurable, name);
                    if (child != null) {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    private boolean isSchemeModified(CodeStyleScheme scheme) {
        for (CodeStyleConfigurableWrapper panel : myPanels) {
            if (panel.isPanelModified(scheme)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Configurable[] buildConfigurables() {
        myPanels = new ArrayList<>();

        List<Configurable> result = new ArrayList<>();

        myProject.getApplication().getExtensionPoint(CodeStyleSettingsProvider.class).forEach(provider -> {
            boolean isGeneral = provider instanceof GeneralCodeStyleSettingsProvider;

            if (provider.hasSettingsPage()) {
                CodeStyleConfigurableWrapper wrapper = buildWrapper(provider);

                myPanels.add(wrapper);

                if (!isGeneral) {
                    result.add(wrapper);
                }
                else {
                    myRootConfigurable = wrapper;
                }
            }
        });

        return result.toArray(new Configurable[result.size()]);
    }

    private CodeStyleConfigurableWrapper buildWrapper(CodeStyleSettingsProvider provider) {
        return new CodeStyleConfigurableWrapper(
            provider,
            new CodeStyleSettingsPanelFactory() {
                @Override
                public NewCodeStyleSettingsPanel createPanel(CodeStyleScheme scheme) {
                    return new NewCodeStyleSettingsPanel(provider.createSettingsPage(
                        scheme.getCodeStyleSettings(),
                        ensureModel().getCloneSettings(scheme)
                    ));
                }
            }
        );
    }

    private CodeStyleSchemesModelImpl ensureModel() {
        if (myModel == null) {
            myModel = new CodeStyleSchemesModelImpl(myProject);
        }
        return myModel;
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return CodeStyleLocalize.configurableSchemesDisplayName();
    }

    @Override
    @RequiredUIAccess
    public boolean isModified() {
        if (myModel != null) {
            boolean schemeListModified = myModel.isSchemeListModified() || myRootConfigurable != null && myRootConfigurable.isModified();
            if (schemeListModified) {
                myApplyCompleted = false;
                myRevertCompleted = false;
            }
            return schemeListModified;
        }

        return false;
    }

    @Nonnull
    @Override
    public String getId() {
        return CONFIGURABLE_ID;
    }

    @Override
    public Set<String> processListOptions() {
        HashSet<String> result = new HashSet<>();
        for (CodeStyleConfigurableWrapper panel : myPanels) {
            result.addAll(panel.processListOptions());
        }
        return result;
    }

    public class CodeStyleConfigurableWrapper implements SearchableConfigurable, NoScroll, NoMargin, OptionsContainingConfigurable {
        private boolean myInitialResetInvoked;
        private CodeStyleMainPanel myPanel;
        private final CodeStyleSettingsProvider myProvider;
        private final CodeStyleSettingsPanelFactory myFactory;

        public CodeStyleConfigurableWrapper(@Nonnull CodeStyleSettingsProvider provider, @Nonnull CodeStyleSettingsPanelFactory factory) {
            myProvider = provider;
            myFactory = factory;
            myInitialResetInvoked = false;
        }

        @Nonnull
        @Override
        public LocalizeValue getDisplayName() {
            LocalizeValue displayName = myProvider.getConfigurableDisplayName();
            if (displayName.isNotEmpty()) {
                return displayName;
            }

            return ensurePanel().getDisplayName();
        }

        @Override
        public String getHelpTopic() {
            return ensurePanel().getHelpTopic();
        }

        private CodeStyleMainPanel ensurePanel() {
            if (myPanel == null) {
                myPanel = new CodeStyleMainPanel(ensureModel(), myFactory);
            }
            return myPanel;
        }

        @Override
        @RequiredUIAccess
        public JComponent createComponent(@Nonnull Disposable uiDisposable) {
            return ensurePanel();
        }

        @Override
        @RequiredUIAccess
        public boolean isModified() {
            boolean someSchemeModified = myPanel != null && myPanel.isModified();
            if (someSchemeModified) {
                myApplyCompleted = false;
                myRevertCompleted = false;
            }
            return someSchemeModified;
        }

        @Override
        @RequiredUIAccess
        public void apply() throws ConfigurationException {
            CodeStyleSchemesConfigurable.this.apply();
        }

        public void resetPanel() {
            if (myPanel != null) {
                myPanel.reset();
            }
        }

        @Override
        public String toString() {
            return myProvider.getClass().getName();
        }

        @Override
        @RequiredUIAccess
        public void reset() {
            if (!myInitialResetInvoked) {
                try {
                    resetFromChild();
                }
                finally {
                    myInitialResetInvoked = true;
                }
            }
            else {
                revert();
            }
        }

        @Nonnull
        @Override
        public String getId() {
            return "preferences.sourceCode." + getDisplayName().getKey().orElse(LocalizeKey.empty()).getLocalizeId();
        }

        @Override
        @RequiredUIAccess
        public void disposeUIResources() {
            if (myPanel != null) {
                myPanel.disposeUIResources();
                myPanel = null;
            }
        }

        public void selectTab(@Nonnull LocalizeValue tabName) {
            assert myPanel != null;
            myPanel.showTabOnCurrentPanel(tabName.get());
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public void selectTab(@Nonnull String tab) {
            assert myPanel != null;
            myPanel.showTabOnCurrentPanel(tab);
        }

        public boolean isPanelModified(CodeStyleScheme scheme) {
            return myPanel != null && myPanel.isModified(scheme);
        }

        public boolean isPanelModified() {
            return myPanel != null && myPanel.isModified();
        }

        public void applyPanel() {
            if (myPanel != null) {
                myPanel.apply();
            }
        }

        @Override
        public Set<String> processListOptions() {
            return ensurePanel().processListOptions();
        }
    }
}
