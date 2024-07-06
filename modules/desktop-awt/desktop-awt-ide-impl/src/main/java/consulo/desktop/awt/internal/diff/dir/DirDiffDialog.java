/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff.dir;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.disposer.Disposer;
import consulo.ui.ex.awt.table.JBTable;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public class DirDiffDialog extends DialogWrapper {
  private final DirDiffTableModel myModel;
  private DirDiffPanel myDiffPanel;

  public DirDiffDialog(Project project, DirDiffTableModel model) {
    super(project);
    setModal(false);
    myModel = model;
    setTitle("Directory Diff");
    init();
    final JBTable table = myDiffPanel.getTable();
    table.setColumnSelectionAllowed(false);
    table.getTableHeader().setReorderingAllowed(false);
    table.getTableHeader().setResizingAllowed(false);
    Disposer.register(getDisposable(), myModel);
    Disposer.register(project, getDisposable());
  }

  @Override
  protected String getDimensionServiceKey() {
    setScalableSize(800, 600);
    myDiffPanel.setupSplitter();
    return "DirDiffDialog";
  }

  @Override
  protected JComponent createCenterPanel() {
    myDiffPanel = new DirDiffPanel(myModel, new DirDiffWindow(this));
    Disposer.register(getDisposable(), myDiffPanel);
    return myDiffPanel.getPanel();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myDiffPanel.getTable();
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[]{};
  }

  @Override
  protected String getHelpId() {
    return "reference.dialogs.diff.folder";
  }
}
