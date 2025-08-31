/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.desktop.awt.wm.navigationToolbar;

import consulo.application.ApplicationManager;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.ide.impl.idea.ide.actions.CopyAction;
import consulo.ide.impl.idea.ide.actions.CutAction;
import consulo.ide.impl.idea.ide.navigationToolbar.NavBarModelListener;
import consulo.ide.impl.idea.openapi.actionSystem.PopupAction;
import consulo.language.editor.wolfAnalyzer.ProblemListener;
import consulo.language.psi.PsiManager;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.language.psi.event.PsiTreeChangeListener;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class NavBarListener
  implements ActionListener, ProblemListener, FocusListener, FileStatusListener, AnActionListener, FileEditorManagerListener,
             PsiTreeChangeListener, ModuleRootListener, NavBarModelListener, PropertyChangeListener, KeyListener, WindowFocusListener {
  private static final String LISTENER = "NavBarListener";
  private static final String BUS = "NavBarMessageBus";
  private final NavBarPanel myPanel;
  private boolean shouldFocusEditor = false;

  static void subscribeTo(NavBarPanel panel) {
    if (panel.getClientProperty(LISTENER) != null) {
      unsubscribeFrom(panel);
    }

    NavBarListener listener = new NavBarListener(panel);
    Project project = panel.getProject();
    panel.putClientProperty(LISTENER, listener);
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(listener);
    FileStatusManager.getInstance(project).addFileStatusListener(listener);
    PsiManager.getInstance(project).addPsiTreeChangeListener(listener);

    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(AnActionListener.class, listener);
    connection.subscribe(ProblemListener.class, listener);
    connection.subscribe(ModuleRootListener.class, listener);
    connection.subscribe(NavBarModelListener.class, listener);
    connection.subscribe(FileEditorManagerListener.class, listener);
    panel.putClientProperty(BUS, connection);
    panel.addKeyListener(listener);

    if (panel.isInFloatingMode()) {
      Window window = SwingUtilities.windowForComponent(panel);
      if (window != null) {
        window.addWindowFocusListener(listener);
      }
    }
  }

  static void unsubscribeFrom(NavBarPanel panel) {
    NavBarListener listener = (NavBarListener)panel.getClientProperty(LISTENER);
    panel.putClientProperty(LISTENER, null);
    if (listener != null) {
      Project project = panel.getProject();
      KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(listener);
      FileStatusManager.getInstance(project).removeFileStatusListener(listener);
      PsiManager.getInstance(project).removePsiTreeChangeListener(listener);
      MessageBusConnection connection = (MessageBusConnection)panel.getClientProperty(BUS);
      panel.putClientProperty(BUS, null);
      if (connection != null) {
        connection.disconnect();
      }
    }
  }

  NavBarListener(NavBarPanel panel) {
    myPanel = panel;
    for (NavBarKeyboardCommand command : NavBarKeyboardCommand.values()) {
      registerKey(command);
    }
    myPanel.addFocusListener(this);
  }

  private void registerKey(NavBarKeyboardCommand cmd) {
    myPanel.registerKeyboardAction(this, cmd.name(), cmd.getKeyStroke(), JComponent.WHEN_FOCUSED);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    NavBarKeyboardCommand cmd = NavBarKeyboardCommand.fromString(e.getActionCommand());
    if (cmd != null) {
      switch (cmd) {
        case LEFT:     myPanel.moveLeft();  break;
        case RIGHT:    myPanel.moveRight(); break;
        case HOME:     myPanel.moveHome();  break;
        case END:      myPanel.moveEnd();   break;
        case DOWN:     myPanel.moveDown();  break;
        case UP:       myPanel.moveDown();  break;
        case ENTER:    myPanel.enter();     break;
        case ESCAPE:   myPanel.escape();    break;
        case NAVIGATE: myPanel.navigate();  break;
      }
    }
  }

  @Override
  public void focusGained(FocusEvent e) {
    if (e.getOppositeComponent() == null && shouldFocusEditor) {
      shouldFocusEditor = false;
      ToolWindowManager.getInstance(myPanel.getProject()).activateEditorComponent();
      return;
    }
    myPanel.updateItems();
    List<NavBarItem> items = myPanel.getItems();
    if (!myPanel.isInFloatingMode() && items.size() > 0) {
      myPanel.setContextComponent(items.get(items.size() - 1));
    } else {
      myPanel.setContextComponent(null);
    }
  }

  @Override
  public void focusLost(final FocusEvent e) {
    if (myPanel.getProject().isDisposed()) {
      myPanel.setContextComponent(null);
      myPanel.hideHint();
      return;
    }
    final DialogWrapper dialog = DialogWrapper.findInstance(e.getOppositeComponent());
    shouldFocusEditor =  dialog != null;
    if (dialog != null) {
      Disposer.register(dialog.getDisposable(), new Disposable() {
        @Override
        public void dispose() {
          if (dialog.getExitCode() == DialogWrapper.CANCEL_EXIT_CODE) {
            shouldFocusEditor = false;
          }
        }
      });
    }

    // required invokeLater since in current call sequence KeyboardFocusManager is not initialized yet
    // but future focused component
    //noinspection SSBasedInspection
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        processFocusLost(e);
      }
    });
  }

  private void processFocusLost(FocusEvent e) {
    Component opposite = e.getOppositeComponent();

    if (myPanel.isInFloatingMode() && opposite != null && DialogWrapper.findInstance(opposite) != null) {
      myPanel.hideHint();
      return;
    }

    boolean nodePopupInactive = !myPanel.isNodePopupActive();
    boolean childPopupInactive = !JBPopupFactory.getInstance().isChildPopupFocused(myPanel);
    if (nodePopupInactive && childPopupInactive) {
      if (opposite != null && opposite != myPanel && !myPanel.isAncestorOf(opposite) && !e.isTemporary()) {
        myPanel.setContextComponent(null);
        myPanel.hideHint();
      }
    }

    myPanel.updateItems();
  }

  private void rebuildUI() {
    if (myPanel.isShowing()) {
      myPanel.getUpdateQueue().queueRebuildUi();
    }
  }

  private void updateModel() {
    if (myPanel.isShowing()) {
      myPanel.getModel().setChanged(true);
      myPanel.getUpdateQueue().queueModelUpdateFromFocus();
    }
  }

  @Override
  public void fileStatusesChanged() {
    rebuildUI();
  }

  @Override
  public void fileStatusChanged(@Nonnull VirtualFile virtualFile) {
    rebuildUI();
  }

  @Override
  public void childAdded(@Nonnull PsiTreeChangeEvent event) {
    updateModel();
  }

  @Override
  public void childReplaced(@Nonnull PsiTreeChangeEvent event) {
    updateModel();
  }

  @Override
  public void childMoved(@Nonnull PsiTreeChangeEvent event) {
    updateModel();
  }

  @Override
  public void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
    updateModel();
  }

  @Override
  public void propertyChanged(@Nonnull PsiTreeChangeEvent event) {
    updateModel();
  }

  @Override
  public void rootsChanged(ModuleRootEvent event) {
    updateModel();
  }

  @Override
  public void problemsAppeared(@Nonnull VirtualFile file) {
    updateModel();
  }

  @Override
  public void problemsDisappeared(@Nonnull VirtualFile file) {
    updateModel();
  }

  @Override
  public void modelChanged() {
    rebuildUI();
  }

  @Override
  public void selectionChanged() {
    myPanel.updateItems();
    myPanel.scrollSelectionToVisible();
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (myPanel.isShowing()) {
      String name = evt.getPropertyName();
      if ("focusOwner".equals(name) || "permanentFocusOwner".equals(name)) {
        myPanel.getUpdateQueue().restartRebuild();
      }
    }
  }
  @Override
  public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
    if (shouldSkipAction(action)) return;

    if (myPanel.isInFloatingMode()) {
      myPanel.hideHint();
    } else {
      myPanel.cancelPopup();
    }
  }

  private static boolean shouldSkipAction(AnAction action) {
    return action instanceof PopupAction
           || action instanceof CopyAction
           || action instanceof CutAction
           || action instanceof ScrollingUtil.ListScrollAction;
  }

  @Override
  public void keyPressed(final KeyEvent e) {
    if (!(e.isAltDown() || e.isMetaDown() || e.isControlDown() || myPanel.isNodePopupActive())) {
      if (!Character.isLetter(e.getKeyChar())) {
        return;
      }

      myPanel.moveDown();
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          try {
            Robot robot = new Robot();
            boolean shiftOn = e.isShiftDown();
            int code = e.getKeyCode();
            if (shiftOn) {
              robot.keyPress(KeyEvent.VK_SHIFT);
            }
            robot.keyPress(code);
            robot.keyRelease(code);
          }
          catch (AWTException ignored) {
          }
        }
      });
    }
  }

  @Override
  public void fileOpened(@Nonnull final FileEditorManager manager, @Nonnull final VirtualFile file) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (myPanel.hasFocus()) {
          manager.openFile(file, true);
        }
      }
    });
  }

  @Override
  public void windowLostFocus(WindowEvent e) {
    Window window = e.getWindow();
    Window oppositeWindow = e.getOppositeWindow();
  }

  //---- Ignored
  @Override
  public void windowGainedFocus(WindowEvent e) {
  }

  @Override
  public void keyTyped(KeyEvent e) {}

  @Override
  public void keyReleased(KeyEvent e) {}

  @Override
  public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {}

  @Override
  public void beforeEditorTyping(char c, DataContext dataContext) {}

  @Override
  public void beforeRootsChange(ModuleRootEvent event) {}

  @Override
  public void beforeChildAddition(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void beforeChildRemoval(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void beforeChildReplacement(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void beforeChildMovement(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void beforeChildrenChange(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void beforePropertyChange(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void childRemoved(@Nonnull PsiTreeChangeEvent event) {}

  @Override
  public void fileClosed(@Nonnull FileEditorManager source, @Nonnull VirtualFile file) {}

  @Override
  public void selectionChanged(@Nonnull FileEditorManagerEvent event) {}
}
