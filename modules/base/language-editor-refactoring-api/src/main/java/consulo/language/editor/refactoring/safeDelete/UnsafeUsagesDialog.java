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

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.LocalizeAction;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author Jeka
 * @since 2001-09-12
 */
public class UnsafeUsagesDialog extends DialogWrapper {
  private JEditorPane myMessagePane;
  private final String[] myConflictDescriptions;
  public static final int VIEW_USAGES_EXIT_CODE = NEXT_USER_EXIT_CODE;

  public UnsafeUsagesDialog(String[] conflictDescriptions, Project project) {
    super(project, true);
    myConflictDescriptions = conflictDescriptions;
    setTitle(RefactoringLocalize.usagesDetected());
    setOKButtonText(RefactoringLocalize.deleteAnywayButton());
    init();
  }

  @Override
  @Nonnull
  protected Action[] createActions() {
    final ViewUsagesAction viewUsagesAction = new ViewUsagesAction();

    final Action ignoreAction = getOKAction();
    ignoreAction.putValue(DialogWrapper.DEFAULT_ACTION, null);

    return new Action[]{viewUsagesAction, ignoreAction, new CancelAction()};
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    myMessagePane = new JEditorPane(UIUtil.HTML_MIME, "");
    myMessagePane.setEditable(false);
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myMessagePane);
    scrollPane.setPreferredSize(new Dimension(500, 400));
    panel.add(new JLabel(RefactoringLocalize.theFollowingProblemsWereFound().get()), BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);

    StringBuilder buf = new StringBuilder();
    for (String description : myConflictDescriptions) {
      buf.append(description);
      buf.append("<br><br>");
    }
    myMessagePane.setText(buf.toString());
    return panel;
  }

  @Override
  protected String getDimensionServiceKey() {
    return "#consulo.language.editor.refactoring.safeDelete.UnsafeUsagesDialog";
  }

/*
  protected JComponent createSouthPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(super.createSouthPanel(), BorderLayout.CENTER);
//    panel.add(new JLabel("Do you wish to ignore them and continue?"), BorderLayout.WEST);
    return panel;
  }
*/

  private class CancelAction extends LocalizeAction {
    public CancelAction() {
      super(RefactoringLocalize.cancelButton());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      doCancelAction();
    }
  }

  private class ViewUsagesAction extends LocalizeAction {
    public ViewUsagesAction() {
      super(RefactoringLocalize.viewUsages());
      putValue(DialogWrapper.DEFAULT_ACTION, Boolean.TRUE);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      close(VIEW_USAGES_EXIT_CODE);
    }
  }
}
