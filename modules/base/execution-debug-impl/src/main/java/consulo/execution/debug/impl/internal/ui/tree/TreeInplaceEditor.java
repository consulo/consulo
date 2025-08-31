/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui.tree;

import consulo.application.ApplicationManager;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.executor.Executor;
import consulo.execution.ui.RunContentDescriptor;
import consulo.execution.ui.event.RunContentWithExecutorListener;
import consulo.language.editor.completion.lookup.LookupEx;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public abstract class TreeInplaceEditor implements AWTEventListener {
  private static final Logger LOG = Logger.getInstance(TreeInplaceEditor.class);
  private JComponent myInplaceEditorComponent;
  private final List<Runnable> myRemoveActions = new ArrayList<>();
  protected final Disposable myDisposable = Disposable.newDisposable();

  protected abstract JComponent createInplaceEditorComponent();

  protected abstract JComponent getPreferredFocusedComponent();

  public abstract Editor getEditor();

  public abstract JComponent getEditorComponent();

  protected abstract TreePath getNodePath();

  protected abstract JTree getTree();

  protected void doPopupOKAction() {
    doOKAction();
  }

  public void doOKAction() {
    hide();
  }

  public void cancelEditing() {
    hide();
  }

  private void hide() {
    if (!isShown()) {
      return;
    }
    myInplaceEditorComponent = null;
    onHidden();
    for (Runnable action : myRemoveActions) {
      action.run();
    }
    myRemoveActions.clear();

    Disposer.dispose(myDisposable);

    JTree tree = getTree();
    tree.repaint();
    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(tree);
  }

  protected void onHidden() {
  }

  protected abstract Project getProject();

  private static void setInplaceEditorBounds(JComponent component, int x, int y, int width, int height) {
    int h = Math.max(height, component.getPreferredSize().height);
    component.setBounds(x, y - (h - height) / 2, width, h);
  }

  public final void show() {
    LOG.assertTrue(myInplaceEditorComponent == null, "editor is not released");
    JTree tree = getTree();
    tree.scrollPathToVisible(getNodePath());
    JRootPane rootPane = tree.getRootPane();
    if (rootPane == null) {
      return;
    }
    JLayeredPane layeredPane = rootPane.getLayeredPane();

    Rectangle bounds = getEditorBounds();
    if (bounds == null) {
      return;
    }
    Point layeredPanePoint = SwingUtilities.convertPoint(tree, bounds.x, bounds.y,layeredPane);

    final JComponent inplaceEditorComponent = createInplaceEditorComponent();
    myInplaceEditorComponent = inplaceEditorComponent;
    LOG.assertTrue(inplaceEditorComponent != null);
    setInplaceEditorBounds(inplaceEditorComponent, layeredPanePoint.x, layeredPanePoint.y, bounds.width, bounds.height);

    layeredPane.add(inplaceEditorComponent, Integer.valueOf(250));

    myRemoveActions.add(() -> layeredPane.remove(inplaceEditorComponent));

    inplaceEditorComponent.validate();
    inplaceEditorComponent.paintImmediately(0,0,inplaceEditorComponent.getWidth(),inplaceEditorComponent.getHeight());
    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(getPreferredFocusedComponent());

    ComponentAdapter componentListener = new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        Project project = getProject();
        ApplicationManager.getApplication().invokeLater(() -> {
          if (!isShown() || project == null || project.isDisposed()) {
            return;
          }
          JTree tree1 = getTree();
          JLayeredPane layeredPane1 = tree1.getRootPane().getLayeredPane();
          Rectangle bounds1 = getEditorBounds();
          if (bounds1 == null) {
            return;
          }
          Point layeredPanePoint1 = SwingUtilities.convertPoint(tree1, bounds1.x, bounds1.y, layeredPane1);
          setInplaceEditorBounds(inplaceEditorComponent, layeredPanePoint1.x, layeredPanePoint1.y, bounds1.width, bounds1.height);
          inplaceEditorComponent.revalidate();
        });
      }

      @Override
      public void componentHidden(ComponentEvent e) {
        cancelEditing();
      }
    };

    HierarchyListener hierarchyListener = e -> {
      if (!tree.isShowing()) {
        cancelEditing();
      }
    };

    tree.addHierarchyListener(hierarchyListener);
    tree.addComponentListener(componentListener);
    rootPane.addComponentListener(componentListener);

    myRemoveActions.add(() -> {
      tree.removeHierarchyListener(hierarchyListener);
      tree.removeComponentListener(componentListener);
      rootPane.removeComponentListener(componentListener);
    });

    getProject().getMessageBus().connect(myDisposable).subscribe(RunContentWithExecutorListener.class, new RunContentWithExecutorListener() {
      @Override
      public void contentSelected(@Nullable RunContentDescriptor descriptor, @Nonnull Executor executor) {
        cancelEditing();
      }

      @Override
      public void contentRemoved(@Nullable RunContentDescriptor descriptor, @Nonnull Executor executor) {
        cancelEditing();
      }
    });

    JComponent editorComponent = getEditorComponent();
    editorComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enterStroke");
    editorComponent.getActionMap().put("enterStroke", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doOKAction();
      }
    });
    editorComponent.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escapeStroke");
    editorComponent.getActionMap().put("escapeStroke", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cancelEditing();
      }
    });
    Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
    SwingUtilities.invokeLater(() -> {
      if (!isShown()) return;
      defaultToolkit.addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
    });

    myRemoveActions.add(() -> defaultToolkit.removeAWTEventListener(this));
    onShown();
  }

  protected void onShown() {
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    if (!isShown()) {
      return;
    }
    MouseEvent mouseEvent = (MouseEvent)event;
    if (mouseEvent.getClickCount() == 0 && !(event instanceof MouseWheelEvent)) {
      return;
    }

    int id = mouseEvent.getID();
    if (id != MouseEvent.MOUSE_PRESSED && id != MouseEvent.MOUSE_RELEASED && id != MouseEvent.MOUSE_CLICKED && id != MouseEvent.MOUSE_WHEEL) {
      return;
    }

    Component sourceComponent = mouseEvent.getComponent();
    Point originalPoint = mouseEvent.getPoint();

    Editor editor = getEditor();
    if (editor == null) return;

    Project project = editor.getProject();
    LookupEx activeLookup = project != null ? LookupManager.getInstance(project).getActiveLookup() : null;
    if (activeLookup != null){
      Point lookupPoint = SwingUtilities.convertPoint(sourceComponent, originalPoint, activeLookup.getComponent());
      if (activeLookup.getComponent().getBounds().contains(lookupPoint)){
        return; //mouse click inside lookup
      } else {
        activeLookup.hide(); //hide popup on mouse position changed
      }
    }

    // do not cancel editing if we click in editor popup
    List<JBPopup> popups = JBPopupFactory.getInstance().getChildPopups(myInplaceEditorComponent);
    for (JBPopup popup : popups) {
      if (SwingUtilities.isDescendingFrom(sourceComponent, popup.getContent())) {
        return;
      }
    }

    Point point = SwingUtilities.convertPoint(sourceComponent, originalPoint, myInplaceEditorComponent);
    if (myInplaceEditorComponent.contains(point)) {
      return;
    }
    Component componentAtPoint = SwingUtilities.getDeepestComponentAt(sourceComponent, originalPoint.x, originalPoint.y);
    for (Component comp = componentAtPoint; comp != null; comp = comp.getParent()) {
      if (comp instanceof ComboPopup) {
        if (id != MouseEvent.MOUSE_WHEEL) {
          doPopupOKAction();
        }
        return;
      }
    }
    cancelEditing();
  }

  @Nullable
  protected Rectangle getEditorBounds() {
    JTree tree = getTree();
    Rectangle bounds = tree.getVisibleRect();
    Rectangle nodeBounds = tree.getPathBounds(getNodePath());
    if (bounds == null || nodeBounds == null) {
      return null;
    }
    bounds.y = nodeBounds.y;
    bounds.height = nodeBounds.height;

    if(nodeBounds.x > bounds.x) {
      bounds.width = bounds.width - nodeBounds.x + bounds.x;
      bounds.x = nodeBounds.x;
    }
    return bounds;
  }

  public boolean isShown() {
    return myInplaceEditorComponent != null;
  }
}
