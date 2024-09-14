/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.analysis;

import consulo.content.scope.SearchScope;
import consulo.disposer.Disposer;
import consulo.find.FindSettings;
import consulo.find.ui.ScopeChooserCombo;
import consulo.language.editor.ui.scope.AnalysisUIOptions;
import consulo.language.editor.ui.awt.RadioUpDownListener;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.TitledSeparator;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ContentRevision;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

/**
 * User: anna
 * Date: Jul 6, 2005
 */
public class BaseAnalysisActionDialog extends DialogWrapper {
  private JPanel myPanel;
  private final String myFileName;
  private final String myModuleName;
  private JRadioButton myProjectButton;
  private JRadioButton myModuleButton;
  private JRadioButton myUncommitedFilesButton;
  private JRadioButton myCustomScopeButton;
  private JRadioButton myFileButton;
  private ScopeChooserCombo myScopeCombo;
  private JCheckBox myInspectTestSource;
  private JComboBox myChangeLists;
  private TitledSeparator myTitledSeparator;
  private final Project myProject;
  private final boolean myRememberScope;
  private final String myAnalysisNoon;
  private ButtonGroup myGroup;

  private static final String ALL = AnalysisScopeLocalize.scopeOptionUncommitedFilesAllChangelistsChoice().get();
  private final AnalysisUIOptions myAnalysisOptions;
  @Nullable
  private final PsiElement myContext;

  public BaseAnalysisActionDialog(
    @Nonnull String title,
    @Nonnull String analysisNoon,
    @Nonnull Project project,
    @Nonnull final AnalysisScope scope,
    final String moduleName,
    final boolean rememberScope,
    @Nonnull AnalysisUIOptions analysisUIOptions,
    @Nullable PsiElement context
  ) {
    super(true);
    Disposer.register(myDisposable, myScopeCombo);
    myAnalysisOptions = analysisUIOptions;
    myContext = context;
    if (!analysisUIOptions.ANALYZE_TEST_SOURCES) {
      myAnalysisOptions.ANALYZE_TEST_SOURCES = scope.isAnalyzeTestsByDefault();
    }
    myProject = project;
    myFileName = scope.getScopeType() == AnalysisScope.PROJECT ? null : scope.getShortenName();
    myModuleName = moduleName;
    myRememberScope = rememberScope;
    myAnalysisNoon = analysisNoon;
    init();
    setTitle(title);
    onScopeRadioButtonPressed();
  }

  @Override
  public void setOKActionEnabled(boolean isEnabled) {
    super.setOKActionEnabled(isEnabled);
  }

  @Override
  protected JComponent createCenterPanel() {
    myTitledSeparator.setText(myAnalysisNoon);

    //include test option
    myInspectTestSource.setSelected(myAnalysisOptions.ANALYZE_TEST_SOURCES);

    //module scope if applicable
    myModuleButton.setText(AnalysisScopeLocalize.scopeOptionModuleWithMnemonic(myModuleName).get());
    boolean useModuleScope = false;
    if (myModuleName != null) {
      useModuleScope = myAnalysisOptions.SCOPE_TYPE == AnalysisScope.MODULE;
      myModuleButton.setSelected(myRememberScope && useModuleScope);
    }

    myModuleButton.setVisible(myModuleName != null && ModuleManager.getInstance(myProject).getModules().length > 1);

    boolean useUncommitedFiles = false;
    final ChangeListManager changeListManager = ChangeListManager.getInstance(myProject);
    final boolean hasVCS = !changeListManager.getAffectedFiles().isEmpty();
    if (hasVCS){
      useUncommitedFiles = myAnalysisOptions.SCOPE_TYPE == AnalysisScope.UNCOMMITTED_FILES;
      myUncommitedFilesButton.setSelected(myRememberScope && useUncommitedFiles);
    }
    myUncommitedFilesButton.setVisible(hasVCS);

    DefaultComboBoxModel model = new DefaultComboBoxModel();
    model.addElement(ALL);
    final List<? extends ChangeList> changeLists = changeListManager.getChangeListsCopy();
    for (ChangeList changeList : changeLists) {
      model.addElement(changeList.getName());
    }
    myChangeLists.setModel(model);
    myChangeLists.setEnabled(myUncommitedFilesButton.isSelected());
    myChangeLists.setVisible(hasVCS);

    //file/package/directory/module scope
    if (myFileName != null) {
      myFileButton.setText(myFileName);
      myFileButton.setMnemonic(myFileName.charAt(getSelectedScopeMnemonic()));
    } else {
      myFileButton.setVisible(false);
    }

    VirtualFile file = PsiUtilBase.getVirtualFile(myContext);
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
    boolean searchInLib = file != null && (fileIndex.isInLibraryClasses(file) || fileIndex.isInLibrarySource(file));

    String preselect = StringUtil.isEmptyOrSpaces(myAnalysisOptions.CUSTOM_SCOPE_NAME)
      ? FindSettings.getInstance().getDefaultScopeName()
      : myAnalysisOptions.CUSTOM_SCOPE_NAME;
    if (searchInLib && GlobalSearchScope.projectScope(myProject).getDisplayName().equals(preselect)) {
      preselect = GlobalSearchScope.allScope(myProject).getDisplayName();
    }
    if (GlobalSearchScope.allScope(myProject).getDisplayName().equals(preselect)
      && myAnalysisOptions.SCOPE_TYPE == AnalysisScope.CUSTOM) {
      myAnalysisOptions.CUSTOM_SCOPE_NAME = preselect;
      searchInLib = true;
    }

    //custom scope
    myCustomScopeButton.setSelected(myRememberScope && myAnalysisOptions.SCOPE_TYPE == AnalysisScope.CUSTOM);

    myScopeCombo.init(myProject, searchInLib, true, preselect);

    //correct selection
    myProjectButton.setSelected(
      myRememberScope && myAnalysisOptions.SCOPE_TYPE == AnalysisScope.PROJECT
        || myFileName == null
    );
    myFileButton.setSelected(
      myFileName != null && (
        !myRememberScope ||
          myAnalysisOptions.SCOPE_TYPE != AnalysisScope.PROJECT
            && !useModuleScope
            && myAnalysisOptions.SCOPE_TYPE != AnalysisScope.CUSTOM
            && !useUncommitedFiles
      )
    );

    myScopeCombo.setEnabled(myCustomScopeButton.isSelected());

    final ActionListener radioButtonPressed = e -> onScopeRadioButtonPressed();
    final Enumeration<AbstractButton> enumeration = myGroup.getElements();
    while (enumeration.hasMoreElements()) {
      enumeration.nextElement().addActionListener(radioButtonPressed);
    }

    //additional panel - inspection profile chooser
    JPanel wholePanel = new JPanel(new BorderLayout());
    wholePanel.add(myPanel, BorderLayout.NORTH);
    final JComponent additionalPanel = getAdditionalActionSettings(myProject);
    if (additionalPanel!= null){
      wholePanel.add(additionalPanel, BorderLayout.CENTER);
    }
    new RadioUpDownListener(
      myProjectButton,
      myModuleButton,
      myUncommitedFilesButton,
      myFileButton,
      myCustomScopeButton
    );
    return wholePanel;
  }

  private int getSelectedScopeMnemonic() {

    final int fileIdx = StringUtil.indexOfIgnoreCase(myFileName, "file", 0);
    if (fileIdx > -1) {
      return fileIdx;
    }

    final int dirIdx = StringUtil.indexOfIgnoreCase(myFileName, "directory", 0);
    if (dirIdx > -1) {
      return dirIdx;
    }

    return 0;
  }

  private void onScopeRadioButtonPressed() {
    myScopeCombo.setEnabled(myCustomScopeButton.isSelected());
    myChangeLists.setEnabled(myUncommitedFilesButton.isSelected());
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    final Enumeration<AbstractButton> enumeration = myGroup.getElements();
    while (enumeration.hasMoreElements()) {
      final AbstractButton button = enumeration.nextElement();
      if (button.isSelected()) {
        return button;
      }
    }
    return myPanel;
  }

  @Nullable
  protected JComponent getAdditionalActionSettings(final Project project) {
    return null;
  }

  public boolean isProjectScopeSelected() {
    return myProjectButton.isSelected();
  }

  public boolean isModuleScopeSelected() {
    return myModuleButton != null && myModuleButton.isSelected();
  }

  public boolean isUncommitedFilesSelected(){
    return myUncommitedFilesButton != null && myUncommitedFilesButton.isSelected();
  }

  @Nullable
  public SearchScope getCustomScope(){
    if (myCustomScopeButton.isSelected()){
      return myScopeCombo.getSelectedScope();
    }
    return null;
  }

  public boolean isInspectTestSources(){
    return myInspectTestSource.isSelected();
  }

  @Nonnull
  public AnalysisScope getScope(
    @Nonnull AnalysisUIOptions uiOptions,
    @Nonnull AnalysisScope defaultScope,
    @Nonnull Project project,
    Module module
  ) {
    AnalysisScope scope;
    if (isProjectScopeSelected()) {
      scope = new AnalysisScope(project);
      uiOptions.SCOPE_TYPE = AnalysisScope.PROJECT;
    }
    else {
      final SearchScope customScope = getCustomScope();
      if (customScope != null) {
        scope = new AnalysisScope(customScope, project);
        uiOptions.SCOPE_TYPE = AnalysisScope.CUSTOM;
        uiOptions.CUSTOM_SCOPE_NAME = customScope.getDisplayName();
      }
      else if (isModuleScopeSelected()) {
        scope = new AnalysisScope(module);
        uiOptions.SCOPE_TYPE = AnalysisScope.MODULE;
      }
      else if (isUncommitedFilesSelected()) {
        final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
        List<VirtualFile> files;
        if (myChangeLists.getSelectedItem() == ALL) {
          files = changeListManager.getAffectedFiles();
        }
        else {
          files = new ArrayList<>();
          for (ChangeList list : changeListManager.getChangeListsCopy()) {
            if (!Comparing.strEqual(list.getName(), (String)myChangeLists.getSelectedItem())) continue;
            final Collection<Change> changes = list.getChanges();
            for (Change change : changes) {
              final ContentRevision afterRevision = change.getAfterRevision();
              if (afterRevision != null) {
                final VirtualFile vFile = afterRevision.getFile().getVirtualFile();
                if (vFile != null) {
                  files.add(vFile);
                }
              }
            }
          }
        }
        scope = new AnalysisScope(project, new HashSet<>(files));
        uiOptions.SCOPE_TYPE = AnalysisScope.UNCOMMITTED_FILES;
      }
      else {
        scope = defaultScope;
        uiOptions.SCOPE_TYPE = defaultScope.getScopeType();//just not project scope
      }
    }
    uiOptions.ANALYZE_TEST_SOURCES = isInspectTestSources();
    scope.setIncludeTestSource(isInspectTestSources());
    scope.setScope(getCustomScope());

    FindSettings.getInstance().setDefaultScopeName(scope.getDisplayName());
    return scope;
  }
}
