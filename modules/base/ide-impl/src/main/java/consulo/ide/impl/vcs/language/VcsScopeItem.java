// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.vcs.language;

import consulo.language.editor.internal.ModelScopeItem;
import consulo.language.editor.scope.AnalysisScope;
import consulo.project.Project;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListManager;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class VcsScopeItem implements ModelScopeItem {
  private final ChangeListManager myChangeListManager;
  private final DefaultComboBoxModel<LocalChangeList> myModel;
  private final Project myProject;

  @Nullable
  public static VcsScopeItem createIfHasVCS(Project project) {
    if (ChangeListManager.getInstance(project).getAffectedFiles().isEmpty()) {
      return null;
    }

    return new VcsScopeItem(project);
  }

  public VcsScopeItem(Project project) {
    myProject = project;
    myChangeListManager = ChangeListManager.getInstance(project);
    assert !myChangeListManager.getAffectedFiles().isEmpty();

    if (ChangesUtil.hasMeaningfulChangelists(myProject)) {
      myModel = new DefaultComboBoxModel<>();
      myModel.addElement(null);
      List<LocalChangeList> changeLists = myChangeListManager.getChangeLists();
      for (LocalChangeList changeList : changeLists) {
        myModel.addElement(changeList);
      }
    }
    else {
      myModel = null;
    }
  }

  @Override
  public AnalysisScope getScope() {
    ChangeList changeList = myModel != null ? (ChangeList)myModel.getSelectedItem() : null;

    List<VirtualFile> files;
    if (changeList == null) {
      files = myChangeListManager.getAffectedFiles();
    }
    else {
      LocalChangeList list = myChangeListManager.findChangeList(changeList.getName());
      if (list != null) {
        files = ChangesUtil.iterateAfterRevisionFiles(list.getChanges()).toList();
      }
      else {
        files = Collections.emptyList();
      }
    }
    return new AnalysisScope(myProject, new HashSet<>(files));
  }

  @Nullable
  public DefaultComboBoxModel<LocalChangeList> getChangeListsModel() {
    return myModel;
  }
}