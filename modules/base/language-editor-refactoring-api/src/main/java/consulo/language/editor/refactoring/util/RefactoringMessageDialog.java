/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.util;

import consulo.application.HelpManager;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.internal.laf.MultiLineLabelUI;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class RefactoringMessageDialog extends DialogWrapper {
  private final String myMessage;
  private final String myHelpTopic;
  private final Icon myIcon;
  private final boolean myIsCancelButtonVisible;

  public RefactoringMessageDialog(String title, String message, String helpTopic, @NonNls String iconId, boolean showCancelButton, Project project) {
    super(project, false);
    setTitle(title);
    myMessage = message;
    myHelpTopic = helpTopic;
    myIsCancelButtonVisible = showCancelButton;
    myIcon = UIManager.getIcon(iconId);
    init();
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    List<Action> actions = new ArrayList<>();
    actions.add(getOKAction());
    if (myIsCancelButtonVisible) {
      actions.add(getCancelAction());
    }
    if (myHelpTopic != null) {
      actions.add(getHelpAction());
    }
    return actions.toArray(new Action[actions.size()]);
  }

  @Override
  protected JComponent createNorthPanel() {
    JLabel label = new JLabel(myMessage);
    label.setUI(new MultiLineLabelUI());

    JPanel panel = new JPanel(new BorderLayout(10, 0));
    if (myIcon != null) {
      panel.add(new JLabel(myIcon), BorderLayout.WEST);
      panel.add(label, BorderLayout.CENTER);
    }
    else {
      panel.add(label, BorderLayout.WEST);
    }
    return panel;
  }

  @Override
  protected JComponent createCenterPanel() {
    return null;
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(myHelpTopic);
  }
}
