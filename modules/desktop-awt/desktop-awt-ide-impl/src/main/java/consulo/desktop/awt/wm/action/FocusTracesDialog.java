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
package consulo.desktop.awt.wm.action;

import consulo.desktop.awt.wm.FocusRequestInfo;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.table.JBTable;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class FocusTracesDialog extends DialogWrapper {
  private JTextPane myStacktrace;
  private JBTable myRequestsTable;
  private JPanel myRootPanel;
  private final List<FocusRequestInfo> myRequests;
  private static final String[] COLUMNS = {"Time", "Forced", "Component"};

  public FocusTracesDialog(Project project, ArrayList<FocusRequestInfo> requests) {
    super(project);
    myRequests = requests;
    setTitle("Focus Traces");
    init();
    final String[][] data = new String[requests.size()][];
    for (int i = 0; i < data.length; i++) {
      final FocusRequestInfo r = requests.get(i);
      data[i] = new String[]{r.getDate(), String.valueOf(r.isForced()), String.valueOf(r.getComponent())};
    }
    myRequestsTable.setModel(new DefaultTableModel(data, COLUMNS));
    final ListSelectionListener selectionListener = new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        final int index = myRequestsTable.getSelectedRow();
        if (-1 < index && index < myRequests.size()) {
          myStacktrace.setText(myRequests.get(index).getStackTrace());
        }
        else {
          myStacktrace.setText("");
        }
      }
    };
    myRequestsTable.getSelectionModel().addListSelectionListener(selectionListener);
    final TableColumnModel columnModel = myRequestsTable.getColumnModel();
    columnModel.getColumn(0).setMaxWidth(120);
    columnModel.getColumn(1).setMaxWidth(60);
    columnModel.getSelectionModel().addListSelectionListener(selectionListener);
    columnModel.setColumnSelectionAllowed(false);
    myRequestsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myRequestsTable.changeSelection(0, 0, false, true);
  }

  @Override
  protected String getDimensionServiceKey() {
    return "ide.internal.focus.trace.dialog";
  }

  @Override
  protected JComponent createCenterPanel() {
    return myRootPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myRequestsTable;
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[] {getOKAction(), getCopyStackTraceAction()};
  }

  private Action getCopyStackTraceAction() {
    return new AbstractAction("&Copy stacktrace") {
      @Override
      public void actionPerformed(ActionEvent e) {
        CopyPasteManager.getInstance().setContents(new StringSelection(myStacktrace.getText()));
      }
    };
  }

  @Override
  public void show() {
    final BorderDrawer drawer = new BorderDrawer();
    drawer.start();
    super.show();
    drawer.setDone();
  }

  class BorderDrawer extends Thread {
    Component prev = null;
    private volatile boolean running = true;
    BorderDrawer() {
      super("Focus Border Drawer");
    }

    @Override
    public void run() {
      try {
        while (running) {
          sleep(100);
          paintBorder();
        }
        if (prev != null) {
          prev.repaint();
        }
      }
      catch (InterruptedException e) {//
      }
    }

    private void paintBorder() {
      final int row = FocusTracesDialog.this.myRequestsTable.getSelectedRow();
      if (row != -1) {
        final FocusRequestInfo info = FocusTracesDialog.this.myRequests.get(row);
        if (prev != null && prev != info.getComponent()) {
          prev.repaint();
        }
        prev = info.getComponent();
        if (prev != null && prev.isDisplayable()) {
          final Graphics g = prev.getGraphics();
          g.setColor(JBColor.RED);
          final Dimension sz = prev.getSize();
          UIUtil.drawDottedRectangle(g, 1, 1, sz.width - 2, sz.height - 2);
        }
      }
    }

    public void setDone() {
      running = false;
    }
  }
}
