// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.find.impl;

import consulo.find.FindModel;
import consulo.find.FindSettings;
import consulo.ui.ex.awt.scopeChooser.ScopeChooserCombo;
import consulo.content.scope.ScopeDescriptor;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.language.util.ModuleUtilCore;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.util.lang.function.Condition;
import consulo.util.lang.Pair;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiBundle;
import consulo.content.scope.SearchScope;
import consulo.ide.impl.idea.ui.SimpleListCellRenderer;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ide.impl.idea.util.Functions;
import consulo.ide.impl.idea.util.ObjectUtils;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.disposer.Disposer;
import consulo.platform.base.localize.FindLocalize;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;

class FindPopupScopeUIImpl implements FindPopupScopeUI {
  static final ScopeType PROJECT = new ScopeType("Project", FindLocalize.findPopupScopeProject(), Image.empty(0));
  static final ScopeType MODULE = new ScopeType("Module", FindLocalize.findPopupScopeModule(), Image.empty(0));
  static final ScopeType DIRECTORY = new ScopeType("Directory", FindLocalize.findPopupScopeDirectory(), Image.empty(0));
  static final ScopeType SCOPE = new ScopeType("Scope", FindLocalize.findPopupScopeScope(), Image.empty(0));

  @Nonnull
  private final FindUIHelper myHelper;
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final FindPopupPanel myFindPopupPanel;
  @Nonnull
  private final Pair<ScopeType, JComponent>[] myComponents;

  private ComboBox<String> myModuleComboBox;
  private FindPopupDirectoryChooser myDirectoryChooser;
  private ScopeChooserCombo myScopeCombo;

  FindPopupScopeUIImpl(@Nonnull FindPopupPanel panel) {
    myHelper = panel.getHelper();
    myProject = panel.getProject();
    myFindPopupPanel = panel;
    initComponents();

    boolean fullVersion = true;
    myComponents = fullVersion
                   ? ContainerUtil.ar(new Pair<>(PROJECT, new JLabel()), new Pair<>(MODULE, shrink(myModuleComboBox)), new Pair<>(DIRECTORY, myDirectoryChooser), new Pair<>(SCOPE, shrink(myScopeCombo)))
                   : ContainerUtil.ar(new Pair<>(SCOPE, shrink(myScopeCombo)), new Pair<>(DIRECTORY, myDirectoryChooser));
  }

  public void initComponents() {
    Module[] modules = ModuleManager.getInstance(myProject).getModules();
    String[] names = new String[modules.length];
    for (int i = 0; i < modules.length; i++) {
      names[i] = modules[i].getName();
    }

    Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
    myModuleComboBox = new ComboBox<>(names);
    myModuleComboBox.setMinimumAndPreferredWidth(JBUIScale.scale(300)); // as ScopeChooser
    myModuleComboBox.setRenderer(SimpleListCellRenderer.create("", Functions.id()));

    ActionListener restartSearchListener = e -> scheduleResultsUpdate();
    myModuleComboBox.addActionListener(restartSearchListener);

    myDirectoryChooser = new FindPopupDirectoryChooser(myFindPopupPanel);

    myScopeCombo = new ScopeChooserCombo();
    Object selection = ObjectUtils.coalesce(myHelper.getModel().getCustomScope(), myHelper.getModel().getCustomScopeName(), FindSettings.getInstance().getDefaultScopeName());
    myScopeCombo.init(myProject, true, true, selection, new Condition<ScopeDescriptor>() {
      //final String projectFilesScopeName = PsiBundle.message("psi.search.scope.project");
      final String moduleFilesScopeName;

      {
        String moduleScopeName = PsiBundle.message("search.scope.module", "");
        final int ind = moduleScopeName.indexOf(' ');
        moduleFilesScopeName = moduleScopeName.substring(0, ind + 1);
      }

      @Override
      public boolean value(ScopeDescriptor descriptor) {
        String display = descriptor.getDisplayName();
        if(display == null) {
          if(descriptor.getClass() != ScopeDescriptor.class) {
            display = descriptor.getClass().getSimpleName();
          }
          else {
            SearchScope scope = descriptor.getScope();
            display = scope == null ? "?" : scope.getClass().getSimpleName();
          }
        }
        return /*!projectFilesScopeName.equals(display) &&*/ !display.startsWith(moduleFilesScopeName);
      }
    });
    myScopeCombo.setBrowseListener(new ScopeChooserCombo.BrowseListener() {

      private FindModel myModelSnapshot;

      @Override
      public void onBeforeBrowseStarted() {
        myModelSnapshot = myHelper.getModel();
        myFindPopupPanel.getCanClose().set(false);
      }

      @Override
      public void onAfterBrowseFinished() {
        if (myModelSnapshot != null) {
          SearchScope scope = myScopeCombo.getSelectedScope();
          if (scope != null) {
            myModelSnapshot.setCustomScope(scope);
          }
          myFindPopupPanel.getCanClose().set(true);
        }
      }
    });
    myScopeCombo.getComboBox().addActionListener(restartSearchListener);
    Disposer.register(myFindPopupPanel.getDisposable(), myScopeCombo);
  }

  @Nonnull
  @Override
  public Pair<ScopeType, JComponent>[] getComponents() {
    return myComponents;
  }

  @Override
  public void applyTo(@Nonnull FindSettings findSettings, @Nonnull FindPopupScopeUI.ScopeType selectedScope) {
    findSettings.setDefaultScopeName(myScopeCombo.getSelectedScopeName());
  }

  @Override
  public void applyTo(@Nonnull FindModel findModel, @Nonnull FindPopupScopeUI.ScopeType selectedScope) {
    if (selectedScope == PROJECT) {
      findModel.setProjectScope(true);
    }
    else if (selectedScope == DIRECTORY) {
      String directory = myDirectoryChooser.getDirectory();
      findModel.setDirectoryName(directory);
    }
    else if (selectedScope == MODULE) {
      findModel.setModuleName((String)myModuleComboBox.getSelectedItem());
    }
    else if (selectedScope == SCOPE) {
      SearchScope selectedCustomScope = myScopeCombo.getSelectedScope();
      String customScopeName = selectedCustomScope == null ? null : selectedCustomScope.getDisplayName();
      findModel.setCustomScopeName(customScopeName);
      findModel.setCustomScope(selectedCustomScope);
      findModel.setCustomScope(true);
    }
  }

  @Nullable
  @Override
  public ValidationInfo validate(@Nonnull FindModel model, FindPopupScopeUI.ScopeType selectedScope) {
    if (selectedScope == DIRECTORY) {
      return myDirectoryChooser.validate(model);
    }
    return null;
  }

  @Override
  public boolean hideAllPopups() {
    final JComboBox[] candidates = {myModuleComboBox, myScopeCombo.getComboBox(), myDirectoryChooser.getComboBox()};
    for (JComboBox candidate : candidates) {
      if (candidate.isPopupVisible()) {
        candidate.hidePopup();
        return true;
      }
    }
    return false;
  }

  @Nonnull
  @Override
  public ScopeType initByModel(@Nonnull FindModel findModel) {
    myDirectoryChooser.initByModel(findModel);

    final String dirName = findModel.getDirectoryName();
    if (!StringUtil.isEmptyOrSpaces(dirName)) {
      VirtualFile dir = LocalFileSystem.getInstance().findFileByPath(dirName);
      if (dir != null) {
        Module module = ModuleUtilCore.findModuleForFile(dir, myProject);
        if (module != null) {
          myModuleComboBox.setSelectedItem(module.getName());
        }
      }
    }

    ScopeType scope = getScope(findModel);
    ScopeType selectedScope = Arrays.stream(myComponents).filter(o -> o.first == scope).findFirst().orElse(null) == null ? myComponents[0].first : scope;
    if (selectedScope == MODULE) {
      myModuleComboBox.setSelectedItem(findModel.getModuleName());
    }
    return selectedScope;
  }

  private static JComponent shrink(JComponent toShrink) {
    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(toShrink, BorderLayout.WEST);
    wrapper.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
    return wrapper;
  }

  private void scheduleResultsUpdate() {
    myFindPopupPanel.scheduleResultsUpdate();
  }

  private ScopeType getScope(FindModel model) {
    if (model.isCustomScope()) {
      return SCOPE;
    }
    if (model.isProjectScope()) {
      return PROJECT;
    }
    if (model.getDirectoryName() != null) {
      return DIRECTORY;
    }
    if (model.getModuleName() != null) {
      return MODULE;
    }
    return PROJECT;
  }
}
