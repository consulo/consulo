package com.intellij.refactoring.introduce.inplace;

import consulo.application.ui.UISettings;
import consulo.project.Project;
import consulo.ide.ui.popup.JBPopupFactory;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import java.awt.event.*;

/**
 * User: anna
 */
public class KeyboardComboSwitcher {

  public static void setupActions(final JComboBox comboBox, final Project project) {
    final boolean toggleStrategy = !UIUtil.isUnderAquaLookAndFeel();
    final boolean[] moveFocusBack = new boolean[] {true};
    comboBox.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (!moveFocusBack[0]) {
          moveFocusBack[0] = true;
          return;
        }

        if (toggleStrategy) {
          final int size = comboBox.getModel().getSize();
          int next = comboBox.getSelectedIndex() + 1;
          if (next < 0 || next >= size) {
            if (!UISettings.getInstance().CYCLE_SCROLLING) {
              return;
            }
            next = (next + size) % size;
          }
          comboBox.setSelectedIndex(next);
          ToolWindowManager.getInstance(project).activateEditorComponent();
        }
        else {
          JBPopupFactory popupFactory = JBPopupFactory.getInstance();
          boolean fromTheSameBalloon = popupFactory.getParentBalloonFor(e.getComponent()) == popupFactory.getParentBalloonFor(e.getOppositeComponent());
          if (!fromTheSameBalloon) {
            comboBox.showPopup();
          }
        }
      }
    });
    comboBox.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        moveFocusBack[0] = false;
      }
    });
    comboBox.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        moveFocusBack[0] = true;
        if (!toggleStrategy && e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getModifiers() == 0) {
          ToolWindowManager.getInstance(project).activateEditorComponent();
        }
      }
    });
    comboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        moveFocusBack[0] = true;
        if (!project.isDisposed()) {
          ToolWindowManager.getInstance(project).activateEditorComponent();
        }
      }
    });
  }
}
