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

package consulo.ide.impl.idea.openapi.command.impl;

import consulo.application.CommonBundle;
import consulo.document.DocumentReference;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collection;

/**
 * @author max
 */
public class CannotUndoReportDialog extends DialogWrapper {
  private JList myProblemFilesList;
  private JPanel myPanel;
  private JLabel myProblemMessageLabel;

  public CannotUndoReportDialog(Project project, @Nls String problemText, Collection<DocumentReference> files) {
    super(project, false);

    DefaultListModel model = new DefaultListModel();
    for (DocumentReference file : files) {
      final VirtualFile vFile = file.getFile();
      if (vFile != null) {
        model.add(0, vFile.getPresentableUrl());
      }
      else {
        model.add(0, "<unknown file>");
      }
    }

    myProblemFilesList.setModel(model);
    setTitle(CommonBundle.message("cannot.undo.dialog.title"));

    myProblemMessageLabel.setText(problemText);
    myProblemMessageLabel.setIcon(TargetAWT.to(Messages.getErrorIcon()));

    init();
  }

  @Nonnull
  protected Action[] createActions() {
    return new Action[]{getOKAction()};
  }

  @Nullable
  protected JComponent createCenterPanel() {
    return myPanel;
  }
}
