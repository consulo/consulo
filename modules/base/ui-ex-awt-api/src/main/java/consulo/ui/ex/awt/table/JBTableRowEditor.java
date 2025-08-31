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
package consulo.ui.ex.awt.table;

import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.util.collection.Lists;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public abstract class JBTableRowEditor extends JPanel {
  public interface RowDocumentListener {
    void documentChanged(DocumentEvent e, int column);
  }

  private final List<RowDocumentListener> myListeners = Lists.newLockFreeCopyOnWriteList();
  private MouseEvent myMouseEvent;

  public abstract void prepareEditor(JTable table, int row);

  public abstract JBTableRow getValue();

  public abstract JComponent getPreferredFocusedComponent();

  public abstract JComponent[] getFocusableComponents();

  public final void addDocumentListener(RowDocumentListener listener) {
    myListeners.add(listener);
  }

  public void fireDocumentChanged(DocumentEvent e, int column) {
    for (RowDocumentListener listener : myListeners) {
      listener.documentChanged(e, column);
    }
  }

  @Nullable
  public final MouseEvent getMouseEvent() {
    if (myMouseEvent != null && myMouseEvent.getClickCount() == 0) return null;
    return myMouseEvent;
  }

  public final void setMouseEvent(@Nullable MouseEvent e) {
    myMouseEvent = e;
  }

  public static JPanel createLabeledPanel(String labelText, JComponent component) {
    JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 4, 2, true, false));
    JBLabel label = new JBLabel(labelText, UIUtil.ComponentStyle.SMALL);
    panel.add(label);
    panel.add(component);
    return panel;
  }

  public class RowEditorChangeListener extends DocumentAdapter {
    private int myColumn;

    public RowEditorChangeListener(int column) {
      myColumn = column;
    }

    @Override
    public void documentChanged(DocumentEvent e) {
      fireDocumentChanged(e, myColumn);
    }
  }
}
