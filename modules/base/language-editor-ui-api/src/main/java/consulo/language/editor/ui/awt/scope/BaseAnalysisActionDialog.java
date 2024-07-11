// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.ui.awt.scope;

import consulo.find.FindSettings;
import consulo.language.editor.internal.ModelScopeItem;
import consulo.language.editor.internal.ModelScopeItemPresenter;
import consulo.language.editor.internal.ModelScopeItemView;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.editor.ui.RadioUpDownListener;
import consulo.language.editor.ui.scope.AnalysisUIOptions;
import consulo.language.psi.PsiElement;
import consulo.language.util.ModuleUtilCore;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class BaseAnalysisActionDialog extends DialogWrapper {
  private final static Logger LOG = Logger.getInstance(BaseAnalysisActionDialog.class);

  @Nonnull
  private final AnalysisUIOptions myOptions;
  private final boolean myRememberScope;
  private final boolean myShowInspectTestSource;
  private final String myScopeTitle;
  @Nonnull
  private final Project myProject;
  private final List<RadioButton> radioButtons = new ArrayList<>();
  private CheckBox myInspectTestSource;
  private CheckBox myAnalyzeInjectedCode;
  private final List<ModelScopeItemView> myViewItems;

  /**
   * @deprecated Use {@link BaseAnalysisActionDialog#BaseAnalysisActionDialog(String, String, Project, List, AnalysisUIOptions, boolean, boolean)} instead.
   */
  @Deprecated
  public BaseAnalysisActionDialog(@Nonnull String title,
                                  @Nonnull String scopeTitle,
                                  @Nonnull Project project,
                                  @Nonnull final AnalysisScope scope,
                                  final String moduleName,
                                  final boolean rememberScope,
                                  @Nonnull AnalysisUIOptions analysisUIOptions,
                                  @Nullable PsiElement context) {
    this(title,
         scopeTitle,
         project,
         standardItems(project,
                       scope,
                       moduleName != null ? ModuleManager.getInstance(project).findModuleByName(moduleName) : null,
                       context),
         analysisUIOptions,
         rememberScope);
  }

  @Nonnull
  public static List<ModelScopeItem> standardItems(@Nonnull Project project,
                                                   @Nonnull AnalysisScope scope,
                                                   @Nullable Module module,
                                                   @Nullable PsiElement context) {
    return ContainerUtil.mapNotNull(
      ModelScopeItemPresenter.EP_NAME.getExtensionList(),
      presenter -> presenter.tryCreate(project, scope, module, context));
  }

  public BaseAnalysisActionDialog(@Nonnull String title,
                                  @Nonnull String scopeTitle,
                                  @Nonnull Project project,
                                  @Nonnull List<? extends ModelScopeItem> items,
                                  @Nonnull AnalysisUIOptions options,
                                  final boolean rememberScope) {
    this(title, scopeTitle, project, items, options, rememberScope, ModuleUtilCore.hasTestSourceRoots(project));
  }

  public BaseAnalysisActionDialog(@Nonnull String title,
                                  @Nonnull String scopeTitle,
                                  @Nonnull Project project,
                                  @Nonnull List<? extends ModelScopeItem> items,
                                  @Nonnull AnalysisUIOptions options,
                                  final boolean rememberScope,
                                  final boolean showInspectTestSource) {
    super(true);
    myScopeTitle = scopeTitle;
    myProject = project;

    myViewItems = ModelScopeItemPresenter.createOrderedViews(items, getDisposable());
    myOptions = options;
    myRememberScope = rememberScope;
    myShowInspectTestSource = showInspectTestSource;

    init();
    setTitle(title);
    setResizable(false);
    setOKButtonText(getOKButtonText());
  }

  @Override
  @RequiredUIAccess
  protected JComponent createCenterPanel() {
    myInspectTestSource = CheckBox.create(AnalysisScopeLocalize.scopeOptionIncludeTestSources());
    myInspectTestSource.setValue(myOptions.ANALYZE_TEST_SOURCES);
    myInspectTestSource.setVisible(myShowInspectTestSource);
    myAnalyzeInjectedCode = CheckBox.create(AnalysisScopeLocalize.scopeOptionAnalyzeInjectedCode());
    myAnalyzeInjectedCode.setValue(myOptions.ANALYZE_INJECTED_CODE);
    myAnalyzeInjectedCode.setVisible(false);
//
//    JPanel panel = new BaseAnalysisActionDialogUI().panel(myScopeTitle, myViewItems, myInspectTestSource,
//                                                          myAnalyzeInjectedCode, radioButtons, myDisposable,
//                                                          getAdditionalActionSettings(myProject));

    VerticalLayout layout = VerticalLayout.create();
    ValueGroup<Boolean> group = ValueGroups.boolGroup();
    for (ModelScopeItemView viewItem : myViewItems) {
      DockLayout dockLayout = DockLayout.create();
      RadioButton button = viewItem.button();
      dockLayout.left(button);

      group.add(button);
      radioButtons.add(button);

      Component additionalComponent = viewItem.additionalComponent();
      if (additionalComponent != null) {
        dockLayout.right(additionalComponent);
      }

      layout.add(dockLayout);
    }

    layout.add(myInspectTestSource);
    layout.add(myAnalyzeInjectedCode);

    extendMainLayout(layout, myProject);
    
    preselectButton();

    new RadioUpDownListener(radioButtons.stream().map(radioButton -> (JRadioButton)TargetAWT.to(radioButton)).toArray(JRadioButton[]::new));

    return (JComponent)TargetAWT.to(layout);
  }

  @RequiredUIAccess
  public void setShowInspectInjectedCode(boolean showInspectInjectedCode) {
    myAnalyzeInjectedCode.setVisible(showInspectInjectedCode);
  }

  @RequiredUIAccess
  public void setAnalyzeInjectedCode(boolean selected) {
    myAnalyzeInjectedCode.setValue(selected);
  }

  @RequiredUIAccess
  private void preselectButton() {
    if (myRememberScope) {
      int type = myOptions.SCOPE_TYPE;
      List<ModelScopeItemView> preselectedScopes = ContainerUtil.filter(myViewItems, x -> x.scopeId() == type);

      if (preselectedScopes.size() >= 1) {
        LOG.assertTrue(preselectedScopes.size() == 1, "preselectedScopes.size() == 1");
        preselectedScopes.get(0).button().setValue(true);
        return;
      }
    }

    List<ModelScopeItemView> candidates = new ArrayList<>();
    for (ModelScopeItemView view : myViewItems) {
      candidates.add(view);
      if (view.scopeId() == AnalysisScope.FILE) {
        break;
      }
    }

    Collections.reverse(candidates);
    for (ModelScopeItemView x : candidates) {
      int scopeType = x.scopeId();
      // skip predefined scopes
      if (scopeType == AnalysisScope.CUSTOM || scopeType == AnalysisScope.UNCOMMITTED_FILES) {
        continue;
      }
      x.button().setValue(true);
      break;
    }
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    for (RadioButton button : radioButtons) {
      if (button.getValueOrError()) {
        return (JComponent)TargetAWT.to(button);
      }
    }
    return super.getPreferredFocusedComponent();
  }

  /**
   * @deprecated Use {@link BaseAnalysisActionDialog#getScope(AnalysisScope)} instead.
   */
  @Deprecated
  public AnalysisScope getScope(@Nonnull AnalysisUIOptions uiOptions,
                                @Nonnull AnalysisScope defaultScope,
                                @Nonnull Project project,
                                Module module) {
    return getScope(defaultScope);
  }

  public boolean isProjectScopeSelected() {
    return myViewItems.stream()
                      .filter(x -> x.scopeId() == AnalysisScope.PROJECT)
                      .findFirst().map(x -> x.button().getValueOrError()).orElse(false);
  }

  public boolean isInspectTestSources() {
    return myInspectTestSource.getValueOrError();
  }

  public boolean isAnalyzeInjectedCode() {
    return !myAnalyzeInjectedCode.isVisible() || myAnalyzeInjectedCode.getValueOrError();
  }

  public AnalysisScope getScope(@Nonnull AnalysisScope defaultScope) {
    AnalysisScope scope = null;
    for (ModelScopeItemView x : myViewItems) {
      if (x.button().getValueOrError()) {
        int type = x.scopeId();
        scope = x.model().getScope();
        if (myRememberScope) {
          myOptions.SCOPE_TYPE = type;
          if (type == AnalysisScope.CUSTOM) {
            myOptions.CUSTOM_SCOPE_NAME = scope.toSearchScope().getDisplayName();
          }
        }
      }
    }
    if (scope == null) {
      scope = defaultScope;
      if (myRememberScope) {
        myOptions.SCOPE_TYPE = scope.getScopeType();
      }
    }

    if (myInspectTestSource.isVisible()) {
      if (myRememberScope) {
        myOptions.ANALYZE_TEST_SOURCES = isInspectTestSources();
      }
      scope.setIncludeTestSource(isInspectTestSources());
    }

    if (myAnalyzeInjectedCode.isVisible()) {
      boolean analyzeInjectedCode = isAnalyzeInjectedCode();
      if (myRememberScope) {
        myOptions.ANALYZE_INJECTED_CODE = analyzeInjectedCode;
      }
      scope.setAnalyzeInjectedCode(analyzeInjectedCode);
    }

    FindSettings.getInstance().setDefaultScopeName(scope.getDisplayName());
    return scope;
  }

  protected void extendMainLayout(@Nonnull VerticalLayout layout, @Nonnull Project project) {
  }

  @Nullable
  protected JComponent getAdditionalActionSettings(@Nonnull Project project) {
    return null;
  }

  @Nonnull
  public String getOKButtonText() {
    return AnalysisScopeLocalize.actionAnalyzeVerb().get();
  }
}
