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

package com.maddyhome.idea.copyright.actions;

import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RecursionDlg extends DialogWrapper {
  public RecursionDlg(Project project, VirtualFile file) {
    super(project, false);

    logger.debug("file=" + file);

    this.project = project;
    this.file = file;

    setupControls();

    init();
  }

  public boolean isAll() {
    return rbAll.isSelected();
  }

  public boolean includeSubdirs() {
    return cbSubdirs.isSelected();
  }

  private void setupControls() {
    setTitle("Update Copyright");

    setOKButtonText("Run");

    ButtonGroup group = new ButtonGroup();
    group.add(rbFile);
    group.add(rbAll);

    rbFile.setMnemonic('F');
    rbAll.setMnemonic('A');
    cbSubdirs.setMnemonic('I');

    if (file.isDirectory()) {
      rbFile.setText("File");
      rbFile.setEnabled(false);
      rbAll.setText("All files in " + file.getPresentableUrl());
      rbAll.setSelected(true);
      cbSubdirs.setSelected(true);
      cbSubdirs.setEnabled(true);
    }
    else {
      VirtualFile parent = file.getParent();
      rbFile.setText("File '" + file.getPresentableUrl() + '\'');
      rbFile.setSelected(true);
      if (parent != null) {
        rbAll.setText("All files in " + parent.getPresentableUrl());
        cbSubdirs.setSelected(true);
        cbSubdirs.setEnabled(false);
      }
      else {
        rbAll.setVisible(false);
        cbSubdirs.setVisible(false);
      }
    }

    VirtualFile check = file;
    if (!file.isDirectory()) {
      check = file.getParent();
    }
    ProjectRootManager prm = ProjectRootManager.getInstance(project);
    ProjectFileIndex pfi = prm.getFileIndex();

    VirtualFile[] children = check != null ? check.getChildren() : VirtualFile.EMPTY_ARRAY;
    boolean hasSubdirs = false;
    for (int i = 0; i < children.length && !hasSubdirs; i++) {
      if (children[i].isDirectory() && !pfi.isExcluded(children[i])) {
        hasSubdirs = true;
      }
    }

    cbSubdirs.setVisible(hasSubdirs);
    if (!hasSubdirs) {
      cbSubdirs.setEnabled(false);
      mainPanel.remove(cbSubdirs);
    }

    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (cbSubdirs.isVisible()) {
          cbSubdirs.setEnabled(rbAll.isSelected());
        }
      }
    };

    rbFile.addActionListener(listener);
    rbAll.addActionListener(listener);
  }

  protected JComponent createCenterPanel() {
    return mainPanel;
  }

	private final VirtualFile file;
  private JPanel mainPanel;
  private JRadioButton rbFile;
  private JRadioButton rbAll;
  private JCheckBox cbSubdirs;
  private final Project project;

  private static final Logger logger = Logger.getInstance(RecursionDlg.class.getName());

}