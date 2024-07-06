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

package consulo.language.editor.refactoring.safeDelete;

import consulo.application.HelpManager;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.RefactoringSettings;
import consulo.language.editor.refactoring.internal.RefactoringInternalHelper;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.util.DeleteUtil;
import consulo.language.editor.refactoring.util.TextOccurrencesUtil;
import consulo.language.psi.PsiElement;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.StateRestoringCheckBoxWrapper;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author dsl
 */
public class SafeDeleteDialog extends DialogWrapper {
  private final Project myProject;
  private final PsiElement[] myElements;
  private final Callback myCallback;

  private StateRestoringCheckBoxWrapper myCbSearchInComments;
  private StateRestoringCheckBoxWrapper myCbSearchTextOccurrences;

  private CheckBox myCbSafeDelete;

  private final SafeDeleteProcessorDelegate myDelegate;

  public interface Callback {
    void run(SafeDeleteDialog dialog);
  }

  public SafeDeleteDialog(Project project, PsiElement[] elements, Callback callback) {
    super(project, true);
    myProject = project;
    myElements = elements;
    myCallback = callback;
    myDelegate = getDelegate();
    setTitle(SafeDeleteHandler.REFACTORING_NAME.get());
    init();
  }

  public boolean isSearchInComments() {
    return myCbSearchInComments.getValue();
  }

  public boolean isSearchForTextOccurences() {
    return myCbSearchTextOccurrences != null && myCbSearchTextOccurrences.getValue();
  }

  @Override
  @Nonnull
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp("refactoring.safeDelete");
  }

  @Override
  @RequiredUIAccess
  protected JComponent createNorthPanel() {
    final JPanel panel = new JPanel(new GridBagLayout());
    final GridBagConstraints gbc = new GridBagConstraints();

    String message = isDelete()
      ? RefactoringBundle.message("prompt.delete.elements")
      : RefactoringBundle.message("search.for.usages.and.delete.elements");
    final String warningMessage = DeleteUtil.generateWarningMessage(message, myElements);

    gbc.insets = JBUI.insets(4, 8);
    gbc.weighty = 1;
    gbc.weightx = 1;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.WEST;
    panel.add(new JLabel(warningMessage), gbc);

    if (isDelete()) {
      gbc.gridy++;
      gbc.gridx = 0;
      gbc.weightx = 0.0;
      gbc.gridwidth = 1;
      gbc.insets = JBUI.insets(4, 8, 0, 8);
      myCbSafeDelete = CheckBox.create(RefactoringLocalize.checkboxSafeDeleteWithUsageSearch());
      panel.add(TargetAWT.to(myCbSafeDelete), gbc);
      myCbSafeDelete.addValueListener(e -> {
        updateControls(myCbSearchInComments);
        updateControls(myCbSearchTextOccurrences);
      });
    }

    gbc.gridy++;
    gbc.gridx = 0;
    gbc.weightx = 0.0;
    gbc.gridwidth = 1;
    myCbSearchInComments = new StateRestoringCheckBoxWrapper(RefactoringLocalize.searchInCommentsAndStrings());
    panel.add(TargetAWT.to(myCbSearchInComments.getComponent()), gbc);

    if (needSearchForTextOccurrences()) {
      gbc.gridx++;
      myCbSearchTextOccurrences = new StateRestoringCheckBoxWrapper(RefactoringLocalize.searchForTextOccurrences());
      panel.add(TargetAWT.to(myCbSearchTextOccurrences.getComponent()), gbc);
    }

    final RefactoringSettings refactoringSettings = RefactoringSettings.getInstance();
    if (myCbSafeDelete != null) {
      myCbSafeDelete.setValue(refactoringSettings.SAFE_DELETE_WHEN_DELETE);
    }
    myCbSearchInComments.setValue(myDelegate != null ? myDelegate.isToSearchInComments(myElements[0]) : refactoringSettings.SAFE_DELETE_SEARCH_IN_COMMENTS);
    if (myCbSearchTextOccurrences != null) {
      myCbSearchTextOccurrences.setValue(myDelegate != null ? myDelegate.isToSearchForTextOccurrences(myElements[0]) : refactoringSettings.SAFE_DELETE_SEARCH_IN_NON_JAVA);
    }
    updateControls(myCbSearchTextOccurrences);
    updateControls(myCbSearchInComments);
    return panel;
  }

  @RequiredUIAccess
  private void updateControls(@Nullable StateRestoringCheckBoxWrapper checkBox) {
    if (checkBox == null) return;
    if (myCbSafeDelete == null || myCbSafeDelete.getValue()) {
      checkBox.makeSelectable();
    }
    else {
      checkBox.makeUnselectable(false);
    }
  }

  protected boolean isDelete() {
    return false;
  }

  @Nullable
  private SafeDeleteProcessorDelegate getDelegate() {
    if (myElements.length == 1) {
      for (SafeDeleteProcessorDelegate delegate : SafeDeleteProcessorDelegate.EP_NAME.getExtensionList()) {
        if (delegate.handlesElement(myElements[0])) {
          return delegate;
        }
      }
    }
    return null;
  }

  @Override
  protected JComponent createCenterPanel() {
    return null;
  }

  private boolean needSearchForTextOccurrences() {
    for (PsiElement element : myElements) {
      if (TextOccurrencesUtil.isSearchTextOccurencesEnabled(element)) {
        return true;
      }
    }
    return false;
  }


  @Override
  @RequiredUIAccess
  protected void doOKAction() {
    if (DumbService.isDumb(myProject)) {
      Messages.showMessageDialog(
        myProject,
        "Safe delete refactoring is not available while indexing is in progress",
        "Indexing",
        null
      );
      return;
    }

    RefactoringInternalHelper.getInstance().disableWriteChecksDuring(() -> {
      if (myCallback != null && isSafeDelete()) {
        myCallback.run(this);
      }
      else {
        super.doOKAction();
      }
    });

    final RefactoringSettings refactoringSettings = RefactoringSettings.getInstance();
    if (myCbSafeDelete != null) {
      refactoringSettings.SAFE_DELETE_WHEN_DELETE = myCbSafeDelete.getValue();
    }
    if (isSafeDelete()) {
      if (myDelegate == null) {
        refactoringSettings.SAFE_DELETE_SEARCH_IN_COMMENTS = isSearchInComments();
        if (myCbSearchTextOccurrences != null) {
          refactoringSettings.SAFE_DELETE_SEARCH_IN_NON_JAVA = isSearchForTextOccurences();
        }
      } else {
        myDelegate.setToSearchInComments(myElements[0], isSearchInComments());

        if (myCbSearchTextOccurrences != null) {
          myDelegate.setToSearchForTextOccurrences(myElements[0], isSearchForTextOccurences());
        }
      }
    }
  }

  private boolean isSafeDelete() {
    return !isDelete() || myCbSafeDelete.getValue();
  }
}
