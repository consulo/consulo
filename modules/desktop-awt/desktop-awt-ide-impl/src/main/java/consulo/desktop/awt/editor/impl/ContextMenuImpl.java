// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.editor.impl;

import consulo.codeEditor.event.EditorMouseEvent;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.desktop.awt.language.editor.DesktopEditorFloatPanel;
import consulo.desktop.awt.language.editor.DesktopEditorPanelLayer;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.util.TimerUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author spleaner
 */
public final class ContextMenuImpl extends JPanel implements Disposable, DesktopEditorPanelLayer {
  public static final String ACTION_GROUP = "EditorContextBarMenu";

  private final JComponent myComponent;
  private ActionGroup myActionGroup;
  private boolean myVisible = false;
  private boolean myShow = false;
  private int myCurrentOpacity;
  private Timer myTimer;
  private DesktopEditorImpl myEditor;
  private boolean myDisposed;
  private ActionToolbar myActionToolbar;

  public ContextMenuImpl(@Nonnull final JScrollPane container, @Nonnull final DesktopEditorImpl editor) {
    setLayout(new BorderLayout());
    myEditor = editor;

    final ActionManager actionManager = ActionManager.getInstance();

    editor.addEditorMouseListener(new EditorMouseListener() {
      @Override
      public void mouseExited(@Nonnull final EditorMouseEvent e) {
        if (!isInsideActivationArea(container, e.getMouseEvent().getPoint())) {
          toggleContextToolbar(false);
        }
      }
    });

    editor.addEditorMouseMotionListener(new EditorMouseMotionListener() {
      @RequiredUIAccess
      @Override
      public void mouseMoved(@Nonnull final EditorMouseEvent e) {
        toggleContextToolbar(isInsideActivationArea(container, e.getMouseEvent().getPoint()));
      }
    });

    AnAction action = actionManager.getAction(ACTION_GROUP);
    if (action == null) {
      action = new DefaultActionGroup();
      actionManager.registerAction(ACTION_GROUP, action);
    }

    if (action instanceof ActionGroup) {
      myActionGroup = (ActionGroup)action;
    }

    myComponent = createComponent();
    add(myComponent);

    setVisible(false);
    setOpaque(false);
  }

  private boolean isInsideActivationArea(JScrollPane container, Point p) {
    final Rectangle r = myEditor.getContentComponent().getBounds();

    final Rectangle activationArea = new Rectangle(0, 0, r.width, r.height);
    return activationArea.contains(p.x, p.y);
  }

  public static boolean mayShowToolbar(@Nullable final Document document) {
    if (document == null) {
      return false;
    }

    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    return file != null && file.isValid();
  }

  private void toggleContextToolbar(final boolean show) {
    if (myShow != show) {
      myShow = show;
      restartTimer();
    }
  }

  private void restartTimer() {
    if (myTimer != null && myTimer.isRunning()) {
      myTimer.stop();
    }

    myTimer = TimerUtil.createNamedTimer("Restart context menu", 500, e -> {
      if (myDisposed) return;

      if (myTimer != null && myTimer.isRunning()) myTimer.stop();

      myActionToolbar.updateActionsImmediately();
      if (((Container)myActionToolbar).getComponentCount() == 0) {
        myShow = false;
        return;
      }

      myTimer = TimerUtil.createNamedTimer("Restart context menu now", 50, e1 -> {
        if (myShow) {
          if (myVisible) {
            scheduleHide();
            return;
          }

          ContextMenuImpl.this.setVisible(true);

          myCurrentOpacity += 20;
          if (myCurrentOpacity >= 100) {
            myCurrentOpacity = 100;
            myVisible = true;
            myTimer.stop();

            scheduleHide();
          }
        }
        else {
          if (!myVisible) {
            if (myTimer != null && myTimer.isRunning()) myTimer.stop();
            return;
          }

          myCurrentOpacity -= 20;
          if (myCurrentOpacity <= 0) {
            myCurrentOpacity = 0;
            myVisible = false;
            setVisible(false);
          }
        }
        repaint();
      });

      myTimer.setRepeats(true);
      myTimer.start();
    });

    myTimer.setRepeats(false);
    myTimer.start();
  }

  @Override
  public void dispose() {
    myDisposed = true;
    myEditor = null;

    if (myTimer != null) {
      myTimer.stop();
      myTimer = null;
    }
  }

  private void scheduleHide() {
    if (myTimer != null && myTimer.isRunning()) {
      myTimer.stop();
    }

    myTimer = TimerUtil.createNamedTimer("Hide context menu", 1500, e -> {
      if (myDisposed) return;

      if (myComponent.isVisible()) {
        final PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo != null) {
          final Point location = pointerInfo.getLocation();
          SwingUtilities.convertPointFromScreen(location, myComponent);
          if (!myComponent.getBounds().contains(location)) {
            toggleContextToolbar(false);
          }
          else {
            scheduleHide();
          }
        }
      }
    });

    myTimer.setRepeats(false);
    myTimer.start();
  }

  private JComponent createComponent() {
    myActionToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.CONTEXT_TOOLBAR, myActionGroup, true);
    myActionToolbar.setTargetComponent(myEditor.getContentComponent());

    ContextMenuPanel contextMenuPanel = new ContextMenuPanel(this);
    JComponent toolbarComponent = myActionToolbar.getComponent();
    toolbarComponent.setOpaque(false);
    contextMenuPanel.add(toolbarComponent);

    return contextMenuPanel;
  }

  @Override
  public int getPositionYInLayer() {
    return 23;
  }

  private static class ContextMenuPanel extends DesktopEditorFloatPanel {
    private final ContextMenuImpl myContextMenu;

    private ContextMenuPanel(final ContextMenuImpl contextMenu) {
      myContextMenu = contextMenu;
    }

    @Override
    protected float getChildrenOpacity() {
      return myContextMenu.myCurrentOpacity / 100f;
    }

    @Override
    protected float getBackgroundOpacity() {
      return myContextMenu.myCurrentOpacity / 600f;
    }
  }
}
