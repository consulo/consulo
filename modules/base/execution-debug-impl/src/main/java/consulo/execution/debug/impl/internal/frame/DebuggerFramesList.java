/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.frame;

import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.ui.ex.OccurenceNavigator;
import consulo.ui.ex.awt.JBList;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * @author nik
 */
public abstract class DebuggerFramesList extends JBList implements OccurenceNavigator {
  public DebuggerFramesList() {
    super(new DefaultListModel());
  }

  protected void doInit() {
    getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setCellRenderer(createListRenderer());
    getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          onFrameChanged(getSelectedValue());
        }
      }
    });

    getEmptyText().setText(XDebuggerLocalize.debuggerFramesNotAvailable());
  }

  @Override
  public void setModel(ListModel model) {
    // do not allow to change model (e.g. to FilteringListModel)
  }

  @Override
  public DefaultListModel getModel() {
    return (DefaultListModel)super.getModel();
  }

  public void clear() {
    getModel().clear();
  }

  public int getElementCount() {
    return getModel().getSize();
  }

  @Override
  public String getNextOccurenceActionName() {
    return XDebuggerLocalize.actionNextFrameText().get();
  }

  @Override
  public String getPreviousOccurenceActionName() {
    return XDebuggerLocalize.actionPreviousFrameText().get();
  }

  @Override
  public OccurenceInfo goNextOccurence() {
    setSelectedIndex(getSelectedIndex() + 1);
    return createInfo();
  }

  @Override
  public OccurenceInfo goPreviousOccurence() {
    setSelectedIndex(getSelectedIndex() - 1);
    return createInfo();
  }

  private OccurenceInfo createInfo() {
    return OccurenceInfo.position(getSelectedIndex(), getElementCount());
  }

  @Override
  public boolean hasNextOccurence() {
    return getSelectedIndex() < getElementCount() - 1;
  }

  @Override
  public boolean hasPreviousOccurence() {
    return getSelectedIndex() > 0;
  }

  protected abstract ListCellRenderer createListRenderer();

  protected abstract void onFrameChanged(Object selectedValue);
}
