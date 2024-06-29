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
package consulo.ide.impl.idea.util.ui;

import consulo.dataContext.DataManager;
import consulo.codeEditor.Editor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.EventObject;

/**
 * @author Konstantin Bulenkov
 */
public class JTableCellEditorHelper {
  private JTableCellEditorHelper() {}

  public static void typeAhead(final JTable table, final EventObject e, final int row, final int column) {
    if (e instanceof KeyEvent) {
      final Runnable r = () -> {
        if (table.getEditingColumn() != column && table.getEditingRow() != row) return;

        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (focusOwner == null || !SwingUtilities.isDescendingFrom(focusOwner, table)) return;

        KeyEvent keyEvent = (KeyEvent)e;
        if (Character.isDefined(keyEvent.getKeyChar())) {
          try {
            selectAll(focusOwner);

            Robot r1 = new Robot();
            r1.keyPress(keyEvent.getKeyCode());
            r1.keyRelease(keyEvent.getKeyCode());
          }
          catch (AWTException ignore) {
          }
        } else {
          selectAll(focusOwner);
        }
      };

      SwingUtilities.invokeLater(r);
    }
  }

  private static void selectAll(Component component) {
    if (component instanceof TextComponent) {
      ((TextComponent)component).selectAll();
    } else {
      Editor editor = DataManager.getInstance().getDataContext(component).getData(Editor.KEY);
      if (editor != null) {
        editor.getSelectionModel().setSelection(0, editor.getDocument().getTextLength());
      }
    }
  }
}
