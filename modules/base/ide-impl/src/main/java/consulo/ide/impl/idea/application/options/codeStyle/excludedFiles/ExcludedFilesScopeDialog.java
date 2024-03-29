// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.application.options.codeStyle.excludedFiles;

import consulo.language.codeStyle.fileSet.FileSetDescriptor;
import consulo.language.codeStyle.fileSet.NamedScopeDescriptor;
import consulo.project.Project;
import consulo.content.scope.NamedScope;
import jakarta.annotation.Nonnull;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class ExcludedFilesScopeDialog extends ExcludedFilesDialogBase {
  private ExcludedFilesScopeForm myForm;
  private DefaultComboBoxModel<String> myScopeListModel;

  public final static int EDIT_SCOPES = NEXT_USER_EXIT_CODE;

  private final Action myEditAction;
  private final List<NamedScope> myAvailableScopes;

  protected ExcludedFilesScopeDialog(@Nonnull Project project, @Nonnull List<NamedScope> availableScopes) {
    super(project);
    myAvailableScopes = availableScopes;
    setTitle("Add Scope");
    myEditAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        close(EDIT_SCOPES);
      }
    };
    myEditAction.putValue(Action.NAME, "Edit Scopes...");
    init();
    fillScopesList(availableScopes);
  }


  private void fillScopesList(@Nonnull List<NamedScope> availableScopes) {
    myScopeListModel = new DefaultComboBoxModel<>();
    for (NamedScope scope : availableScopes) {
      myScopeListModel.addElement(scope.getName());
    }
    myForm.getScopesList().setModel(myScopeListModel);
  }


  @Nullable
  @Override
  public FileSetDescriptor getDescriptor() {
    int selectedIndex = myForm.getScopesList().getSelectedIndex();
    String scopeName = selectedIndex >= 0 ? myScopeListModel.getElementAt(selectedIndex) : null;
    if (scopeName != null) {
      for (NamedScope scope : myAvailableScopes) {
        if (scopeName.equals(scope.getName())) {
          return new NamedScopeDescriptor(scope);
        }
      }
    }
    return null;
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    myForm = new ExcludedFilesScopeForm();
    return myForm.getTopPanel();
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), myEditAction};
  }

}
