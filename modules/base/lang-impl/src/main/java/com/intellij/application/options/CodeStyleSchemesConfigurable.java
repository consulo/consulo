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

package com.intellij.application.options;

import com.intellij.application.options.codeStyle.CodeStyleMainPanel;
import com.intellij.application.options.codeStyle.CodeStyleSchemesModel;
import com.intellij.application.options.codeStyle.CodeStyleSettingsPanelFactory;
import com.intellij.application.options.codeStyle.NewCodeStyleSettingsPanel;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsProvider;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemeImpl;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CodeStyleSchemesConfigurable extends SearchableConfigurable.Parent.Abstract implements OptionsContainingConfigurable, Configurable.NoMargin {

  private CodeStyleSchemesModel myModel;
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

  @RequiredUIAccess
  @Override
  public JComponent createComponent() {
    myModel = ensureModel();

    if (myRootConfigurable != null) {
      return myRootConfigurable.createComponent();
    }
    return null;
  }

  @Override
  public boolean hasOwnContent() {
    return true;
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    if (myPanels != null) {
      try {
        super.disposeUIResources();
        for (CodeStyleConfigurableWrapper panel : myPanels) {
          panel.disposeUIResources();
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

  @RequiredUIAccess
  @Override
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
        if (panel.isPanelModified()) return true;
      }
    }

    return false;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    if (!myApplyCompleted) {
      try {
        super.apply();

        for (CodeStyleScheme scheme : new ArrayList<>(myModel.getSchemes())) {
          final boolean isDefaultModified = CodeStyleSchemesModel.cannotBeModified(scheme) && isSchemeModified(scheme);
          if (isDefaultModified) {
            CodeStyleScheme newscheme = myModel.createNewScheme(null, scheme);
            CodeStyleSettings settingsWillBeModified = scheme.getCodeStyleSettings();
            CodeStyleSettings notModifiedSettings = settingsWillBeModified.clone();
            ((CodeStyleSchemeImpl)scheme).setCodeStyleSettings(notModifiedSettings);
            ((CodeStyleSchemeImpl)newscheme).setCodeStyleSettings(settingsWillBeModified);
            myModel.addScheme(newscheme, false);

            if (myModel.getSelectedScheme() == scheme) {
              myModel.selectScheme(newscheme, this);
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
  public SearchableConfigurable findSubConfigurable(@Nonnull final String name) {
    return findSubConfigurable(this, name);
  }

  private static SearchableConfigurable findSubConfigurable(SearchableConfigurable.Parent topConfigurable, @Nonnull final String name) {
    for (Configurable configurable : topConfigurable.getConfigurables()) {
      if (configurable instanceof SearchableConfigurable) {
        if (name.equals(configurable.getDisplayName())) return (SearchableConfigurable)configurable;
        if (configurable instanceof SearchableConfigurable.Parent) {
          SearchableConfigurable child = findSubConfigurable((Parent)configurable, name);
          if (child != null) return child;
        }
      }
    }
    return null;
  }

  private boolean isSchemeModified(final CodeStyleScheme scheme) {
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

    for (CodeStyleSettingsProvider provider : CodeStyleSettingsProvider.EXTENSION_POINT_NAME.getExtensions()) {
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
    }

    return result.toArray(new Configurable[result.size()]);
  }

  private CodeStyleConfigurableWrapper buildWrapper(CodeStyleSettingsProvider provider) {
    return new CodeStyleConfigurableWrapper(provider, new CodeStyleSettingsPanelFactory() {
      @Override
      public NewCodeStyleSettingsPanel createPanel(final CodeStyleScheme scheme) {
        return new NewCodeStyleSettingsPanel(provider.createSettingsPage(scheme.getCodeStyleSettings(), ensureModel().getCloneSettings(scheme)));
      }
    });
  }

  private CodeStyleSchemesModel ensureModel() {
    if (myModel == null) {
      myModel = new CodeStyleSchemesModel(myProject);
    }
    return myModel;
  }

  @Override
  public String getDisplayName() {
    return "Code Style";
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    if (myModel != null) {
      boolean schemeListModified = myModel.isSchemeListModified() ||myRootConfigurable != null && myRootConfigurable.isModified();
      if (schemeListModified) {
        myApplyCompleted = false;
        myRevertCompleted = false;
      }
      return schemeListModified;
    }

    return false;
  }

  @Override
  @Nonnull
  public String getId() {
    return "preferences.sourceCode";
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

    @Override
    @Nls
    public String getDisplayName() {
      String displayName = myProvider.getConfigurableDisplayName();
      if (displayName != null) return displayName;

      return ensurePanel().getDisplayName();  // fallback for 8.0 API compatibility
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

    @RequiredUIAccess
    @Override
    public JComponent createComponent() {
      return ensurePanel();
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
      boolean someSchemeModified = myPanel != null && myPanel.isModified();
      if (someSchemeModified) {
        myApplyCompleted = false;
        myRevertCompleted = false;
      }
      return someSchemeModified;
    }

    @RequiredUIAccess
    @Override
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

    @RequiredUIAccess
    @Override
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

    @Override
    @Nonnull
    public String getId() {
      return "preferences.sourceCode." + getDisplayName();
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
      if (myPanel != null) {
        myPanel.disposeUIResources();
      }
    }

    public void selectTab(@Nonnull String tab) {
      createComponent();
      myPanel.showTabOnCurrentPanel(tab);
    }

    public boolean isPanelModified(CodeStyleScheme scheme) {
      return myPanel != null && myPanel.isModified(scheme);
    }

    public boolean isPanelModified() {
      return myPanel != null && myPanel.isModified();
    }

    public void applyPanel() {
      if(myPanel != null) {
        myPanel.apply();
      }
    }

    @Override
    public Set<String> processListOptions() {
      return ensurePanel().processListOptions();
    }
  }
}
