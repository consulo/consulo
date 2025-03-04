// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.find;

import consulo.content.scope.ScopeDescriptor;
import consulo.content.scope.SearchScope;
import consulo.disposer.Disposer;
import consulo.find.FindModel;
import consulo.find.FindSettings;
import consulo.find.localize.FindLocalize;
import consulo.find.ui.ScopeChooserCombo;
import consulo.ide.impl.idea.find.impl.FindUIHelper;
import consulo.ide.impl.idea.util.Functions;
import consulo.language.psi.PsiBundle;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.JBUIScale;
import consulo.ui.ex.awt.SimpleListCellRenderer;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.ui.ex.awt.internal.ComboBoxStyle;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

class FindPopupScopeUIImpl implements FindPopupScopeUI {
  static final ScopeType PROJECT = new ScopeType("Project", FindLocalize.findPopupScopeProject());
  static final ScopeType MODULE = new ScopeType("Module", FindLocalize.findPopupScopeModule());
  static final ScopeType DIRECTORY = new ScopeType("Directory", FindLocalize.findPopupScopeDirectory());
  static final ScopeType SCOPE = new ScopeType("Scope", FindLocalize.findPopupScopeScope());

  @Nonnull
  private final FindUIHelper myHelper;
  @Nonnull
  private final Project myProject;
  @Nonnull
  private final FindPopupPanel myFindPopupPanel;
  @Nonnull
  private final List<Pair<ScopeType, JComponent>> myComponents;

  private ComboBox<String> myModuleComboBox;
  private FindPopupDirectoryChooser myDirectoryChooser;
  private ScopeChooserCombo myScopeCombo;

  FindPopupScopeUIImpl(@Nonnull FindPopupPanel panel) {
    myHelper = panel.getHelper();
    myProject = panel.getProject();
    myFindPopupPanel = panel;
    initComponents();

    myComponents = List.of(
        Pair.create(PROJECT, new JLabel()),
        Pair.create(MODULE, myModuleComboBox),
        Pair.create(DIRECTORY, myDirectoryChooser.getComboBox()),
        Pair.create(SCOPE, myScopeCombo)
    );
  }

  public void initComponents() {
    Module[] modules = ModuleManager.getInstance(myProject).getModules();
    String[] names = new String[modules.length];
    for (int i = 0; i < modules.length; i++) {
      names[i] = modules[i].getName();
    }

    Arrays.sort(names, String.CASE_INSENSITIVE_ORDER);
    myModuleComboBox = new ComboBox<>(names);
    ComboBoxStyle.makeBorderInline(myModuleComboBox);
    myModuleComboBox.setMinimumAndPreferredWidth(JBUIScale.scale(300)); // as ScopeChooser
    myModuleComboBox.setRenderer(SimpleListCellRenderer.create("", Functions.id()));

    ActionListener restartSearchListener = e -> scheduleResultsUpdate();
    myModuleComboBox.addActionListener(restartSearchListener);

    myDirectoryChooser = new FindPopupDirectoryChooser(myFindPopupPanel);

    myScopeCombo = new ScopeChooserCombo();
    ComboBoxStyle.makeBorderInline(myScopeCombo.getComboBox());

    Object selection = ObjectUtil.coalesce(myHelper.getModel().getCustomScope(), myHelper.getModel().getCustomScopeName(), FindSettings.getInstance().getDefaultScopeName());
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
  public List<Pair<ScopeType, JComponent>> getComponents() {
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
    ScopeType selectedScope = myComponents.stream().filter(o -> o.first == scope).findFirst().orElse(null) == null ? myComponents.get(0).first : scope;
    if (selectedScope == MODULE) {
      myModuleComboBox.setSelectedItem(findModel.getModuleName());
    }
    return selectedScope;
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
