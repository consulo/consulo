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
import consulo.language.editor.refactoring.util.DeleteUtil;
import consulo.language.editor.refactoring.util.TextOccurrencesUtil;
import consulo.language.psi.PsiElement;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.StateRestoringCheckBox;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author dsl
 */
public class SafeDeleteDialog extends DialogWrapper {
  private final Project myProject;
  private final PsiElement[] myElements;
  private final Callback myCallback;

  private StateRestoringCheckBox myCbSearchInComments;
  private StateRestoringCheckBox myCbSearchTextOccurrences;

  private JCheckBox myCbSafeDelete;

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
    setTitle(SafeDeleteHandler.REFACTORING_NAME);
    init();
  }

  public boolean isSearchInComments() {
    return myCbSearchInComments.isSelected();
  }

  public boolean isSearchForTextOccurences() {
    if (myCbSearchTextOccurrences != null) {
      return myCbSearchTextOccurrences.isSelected();
    }
    return false;
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
  protected JComponent createNorthPanel() {
    final JPanel panel = new JPanel(new GridBagLayout());
    final GridBagConstraints gbc = new GridBagConstraints();

    final String promptKey = isDelete() ? "prompt.delete.elements" : "search.for.usages.and.delete.elements";
    final String warningMessage = DeleteUtil.generateWarningMessage(RefactoringBundle.message(promptKey), myElements);

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
      myCbSafeDelete = new JCheckBox(RefactoringBundle.message("checkbox.safe.delete.with.usage.search"));
      panel.add(myCbSafeDelete, gbc);
      myCbSafeDelete.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          updateControls(myCbSearchInComments);
          updateControls(myCbSearchTextOccurrences);
        }
      });
    }

    gbc.gridy++;
    gbc.gridx = 0;
    gbc.weightx = 0.0;
    gbc.gridwidth = 1;
    myCbSearchInComments = new StateRestoringCheckBox();
    myCbSearchInComments.setText(RefactoringBundle.getSearchInCommentsAndStringsText());
    panel.add(myCbSearchInComments, gbc);

    if (needSearchForTextOccurrences()) {
      gbc.gridx++;
      myCbSearchTextOccurrences = new StateRestoringCheckBox();
      myCbSearchTextOccurrences.setText(RefactoringBundle.getSearchForTextOccurrencesText());
      panel.add(myCbSearchTextOccurrences, gbc);
    }

    final RefactoringSettings refactoringSettings = RefactoringSettings.getInstance();
    if (myCbSafeDelete != null) {
      myCbSafeDelete.setSelected(refactoringSettings.SAFE_DELETE_WHEN_DELETE);
    }
    myCbSearchInComments.setSelected(myDelegate != null ? myDelegate.isToSearchInComments(myElements[0]) : refactoringSettings.SAFE_DELETE_SEARCH_IN_COMMENTS);
    if (myCbSearchTextOccurrences != null) {
      myCbSearchTextOccurrences.setSelected(myDelegate != null ? myDelegate.isToSearchForTextOccurrences(myElements[0]) : refactoringSettings.SAFE_DELETE_SEARCH_IN_NON_JAVA);
    }
    updateControls(myCbSearchTextOccurrences);
    updateControls(myCbSearchInComments);
    return panel;
  }

  private void updateControls(@Nullable StateRestoringCheckBox checkBox) {
    if (checkBox == null) return;
    if (myCbSafeDelete == null || myCbSafeDelete.isSelected()) {
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
  protected void doOKAction() {
    if (DumbService.isDumb(myProject)) {
      Messages.showMessageDialog(myProject, "Safe delete refactoring is not available while indexing is in progress", "Indexing", null);
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
      refactoringSettings.SAFE_DELETE_WHEN_DELETE = myCbSafeDelete.isSelected();
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
    if (isDelete()) {
      return myCbSafeDelete.isSelected();
    }
    return true;
  }
}
