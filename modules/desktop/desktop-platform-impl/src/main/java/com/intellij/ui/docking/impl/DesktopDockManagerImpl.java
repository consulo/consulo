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
package com.intellij.ui.docking.impl;

import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.impl.DockableEditorContainerFactory;
import com.intellij.openapi.fileEditor.impl.DesktopDockableEditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.EditorTabbedContainer;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.util.AsyncResult;
import com.intellij.openapi.util.BusyObject;
import com.intellij.openapi.util.MutualMap;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.awt.RelativeRectangle;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.components.panels.VerticalBox;
import com.intellij.ui.docking.DockContainer;
import com.intellij.ui.docking.DockContainerFactory;
import com.intellij.ui.docking.DockableContent;
import com.intellij.ui.docking.DragSession;
import com.intellij.ui.tabs.impl.JBTabsImpl;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.UiNotifyConnector;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.EditorWindow;
import consulo.ui.UIAccess;
import consulo.ui.docking.BaseDockManager;
import consulo.ui.popup.JWindowPopupFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
@State(name = "DockManager", storages = @Storage(file = StoragePathMacros.WORKSPACE_FILE))
public class DesktopDockManagerImpl extends BaseDockManager {
  private final MutualMap<DockContainer, DockWindow> myWindows = new MutualMap<>();

  private MyDragSession myCurrentDragSession;

  private final BusyObject.Impl myBusyObject = new BusyObject.Impl() {
    @Override
    public boolean isReady() {
      return myCurrentDragSession == null;
    }
  };

  @Inject
  public DesktopDockManagerImpl(Project project) {
    super(project);
  }

  @Override
  public IdeFrame getIdeFrame(DockContainer container) {
    Component parent = UIUtil.findUltimateParent(container.getContainerComponent());
    if(parent instanceof Window) {
      consulo.ui.Window uiWindow = TargetAWT.from((Window)parent);

      return uiWindow.getUserData(IdeFrame.KEY);
    }
    return null;
  }

  @Override
  public String getDimensionKeyForFocus(@Nonnull String key) {
    Component owner = IdeFocusManager.getInstance(myProject).getFocusOwner();
    if (owner == null) return key;

    DockWindow wnd = myWindows.getValue(getContainerFor(owner));

    return wnd != null ? key + "#" + wnd.myId : key;
  }

  @Override
  public DockContainer getContainerFor(consulo.ui.Component c) {
    return getContainerFor(TargetAWT.to(c));
  }

  @Override
  public DockContainer getContainerFor(Component c) {
    if (c == null) return null;

    for (DockContainer eachContainer : myContainers) {
      if (SwingUtilities.isDescendingFrom(c, eachContainer.getContainerComponent())) {
        return eachContainer;
      }
    }

    Component parent = UIUtil.findUltimateParent(c);
    if (parent == null) return null;

    for (DockContainer eachContainer : myContainers) {
      if (parent == UIUtil.findUltimateParent(eachContainer.getContainerComponent())) {
        return eachContainer;
      }
    }

    return null;
  }

  @Override
  public DragSession createDragSession(MouseEvent mouseEvent, @Nonnull DockableContent content) {
    stopCurrentDragSession();

    for (DockContainer each : myContainers) {
      if (each.isEmpty() && each.isDisposeWhenEmpty()) {
        DockWindow window = myWindows.getValue(each);
        if (window != null) {
          window.setTransparent(true);
        }
      }
    }

    myCurrentDragSession = new MyDragSession(mouseEvent, content);
    return myCurrentDragSession;
  }


  public void stopCurrentDragSession() {
    if (myCurrentDragSession != null) {
      myCurrentDragSession.cancelSession();
      myCurrentDragSession = null;
      myBusyObject.onReady();

      for (DockContainer each : myContainers) {
        if (!each.isEmpty()) {
          DockWindow window = myWindows.getValue(each);
          if (window != null) {
            window.setTransparent(false);
          }
        }
      }
    }
  }

  private AsyncResult<Void> getReady() {
    return myBusyObject.getReady(this);
  }

  private class MyDragSession implements DragSession {

    private final JWindow myWindow;

    private Image myDragImage;
    private final Image myDefaultDragImage;

    @Nonnull
    private final DockableContent myContent;

    private DockContainer myCurrentOverContainer;
    private final JLabel myImageContainer;

    private MyDragSession(MouseEvent me, @Nonnull DockableContent content) {
      myWindow = JWindowPopupFactory.getInstance().create(null);
      myContent = content;

      Image previewImage = content.getPreviewImage();

      double requiredSize = 220;

      double width = previewImage.getWidth(null);
      double height = previewImage.getHeight(null);

      double ratio;
      if (width > height) {
        ratio = requiredSize / width;
      }
      else {
        ratio = requiredSize / height;
      }

      BufferedImage buffer = UIUtil.createImage(myWindow, (int)width, (int)height, BufferedImage.TYPE_INT_ARGB);
      buffer.createGraphics().drawImage(previewImage, 0, 0, (int)width, (int)height, null);

      myDefaultDragImage = buffer.getScaledInstance((int)(width * ratio), (int)(height * ratio), Image.SCALE_SMOOTH);
      myDragImage = myDefaultDragImage;

      myWindow.getContentPane().setLayout(new BorderLayout());
      myImageContainer = new JLabel(IconUtil.createImageIcon(myDragImage));
      myImageContainer.setBorder(new LineBorder(Color.lightGray));
      myWindow.getContentPane().add(myImageContainer, BorderLayout.CENTER);

      setLocationFrom(me);

      myWindow.setVisible(true);

      WindowManagerEx.getInstanceEx().setAlphaModeEnabled(myWindow, true);
      WindowManagerEx.getInstanceEx().setAlphaModeRatio(myWindow, 0.1f);
      myWindow.getRootPane().putClientProperty("Window.shadow", Boolean.FALSE);
    }

    private void setLocationFrom(MouseEvent me) {
      Point showPoint = me.getPoint();
      SwingUtilities.convertPointToScreen(showPoint, me.getComponent());

      Dimension size = myImageContainer.getSize();
      showPoint.x -= size.width / 2;
      showPoint.y -= size.height / 2;
      myWindow.setBounds(new Rectangle(showPoint, size));
    }

    @Nonnull
    @Override
    public DockContainer.ContentResponse getResponse(MouseEvent e) {
      RelativePoint point = new RelativePoint(e);
      for (DockContainer each : myContainers) {
        RelativeRectangle rec = each.getAcceptArea();
        if (rec.contains(point)) {
          DockContainer.ContentResponse response = each.getContentResponse(myContent, point);
          if (response.canAccept()) {
            return response;
          }
        }
      }
      return DockContainer.ContentResponse.DENY;
    }

    @Override
    public void process(MouseEvent e) {
      RelativePoint point = new RelativePoint(e);

      Image img = null;
      if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
        DockContainer over = findContainerFor(point, myContent);
        if (myCurrentOverContainer != null && myCurrentOverContainer != over) {
          myCurrentOverContainer.resetDropOver(myContent);
          myCurrentOverContainer = null;
        }

        if (myCurrentOverContainer == null && over != null) {
          myCurrentOverContainer = over;
          img = myCurrentOverContainer.startDropOver(myContent, point);
        }

        if (myCurrentOverContainer != null) {
          img = myCurrentOverContainer.processDropOver(myContent, point);
        }

        if (img == null) {
          img = myDefaultDragImage;
        }

        if (img != myDragImage) {
          myDragImage = img;
          myImageContainer.setIcon(IconUtil.createImageIcon(myDragImage));
          myWindow.pack();
        }

        setLocationFrom(e);
      }
      else if (e.getID() == MouseEvent.MOUSE_RELEASED) {
        if (myCurrentOverContainer == null) {
          createNewDockContainerFor(myContent, point);
          stopCurrentDragSession();
        }
        else {
          myCurrentOverContainer.add(myContent, point);
          stopCurrentDragSession();
        }
      }
    }

    @Override
    public void cancel() {
      stopCurrentDragSession();
    }

    private void cancelSession() {
      myWindow.dispose();

      if (myCurrentOverContainer != null) {
        myCurrentOverContainer.resetDropOver(myContent);
        myCurrentOverContainer = null;
      }
    }
  }

  @Nullable
  private DockContainer findContainerFor(RelativePoint point, @Nonnull DockableContent content) {
    for (DockContainer each : myContainers) {
      RelativeRectangle rec = each.getAcceptArea();
      if (rec.contains(point) && each.getContentResponse(content, point).canAccept()) {
        return each;
      }
    }

    for (DockContainer each : myContainers) {
      RelativeRectangle rec = each.getAcceptAreaFallback();
      if (rec.contains(point) && each.getContentResponse(content, point).canAccept()) {
        return each;
      }
    }

    return null;
  }


  private DockContainerFactory getFactory(String type) {
    assert myFactories.containsKey(type) : "No factory for content type=" + type;
    return myFactories.get(type);
  }

  @Override
  public void createNewDockContainerFor(DockableContent content, RelativePoint point) {
    DockContainer container = getFactory(content.getDockContainerType()).createContainer(content);
    register(container);

    final DockWindow window = createWindowFor(null, container);

    Dimension size = content.getPreferredSize();
    Point showPoint = point.getScreenPoint();
    showPoint.x -= size.width / 2;
    showPoint.y -= size.height / 2;

    Rectangle target = new Rectangle(showPoint, size);
    ScreenUtil.moveRectangleToFitTheScreen(target);
    ScreenUtil.cropRectangleToFitTheScreen(target);


    window.setLocation(target.getLocation());
    window.myDockContentUiContainer.setPreferredSize(target.getSize());

    window.show(false);
    window.getFrame().pack();

    container.add(content, new RelativePoint(target.getLocation()));

    SwingUtilities.invokeLater(() -> window.myUiContainer.setPreferredSize(null));
  }

  @Override
  @Nonnull
  public Pair<FileEditor[], FileEditorProvider[]> createNewDockContainerFor(@Nonnull VirtualFile file, @Nonnull FileEditorManagerImpl fileEditorManager) {
    DockContainer container = getFactory(DockableEditorContainerFactory.TYPE).createContainer(null);
    register(container);

    final DockWindow window = createWindowFor(null, container);

    window.show(true);
    final EditorWindow editorWindow = ((DesktopDockableEditorTabbedContainer)container).getSplitters().getOrCreateCurrentWindow(file);
    final Pair<FileEditor[], FileEditorProvider[]> result = fileEditorManager.openFileImpl2(UIAccess.current(), editorWindow, file, true);
    container.add(EditorTabbedContainer.createDockableEditor(myProject, null, file, new Presentation(file.getName()), editorWindow), null);

    SwingUtilities.invokeLater(() -> window.myUiContainer.setPreferredSize(null));
    return result;
  }

  @Override
  protected DockWindow createWindowFor(@Nullable String id, DockContainer container) {
    String windowId = id != null ? id : String.valueOf(myWindowIdCounter++);
    DockWindow window = new DockWindow(windowId, myProject, container, container instanceof DockContainer.Dialog);
    window.setDimensionKey("dock-window-" + windowId);
    myWindows.put(container, window);
    return window;
  }

  private class DockWindow extends FrameWrapper implements IdeEventQueue.EventDispatcher, BaseDockManager.DockWindow {

    private final String myId;
    private final DockContainer myContainer;

    private final VerticalBox myNorthPanel = new VerticalBox();
    private final Map<String, IdeRootPaneNorthExtension> myNorthExtensions = new LinkedHashMap<>();

    private final NonOpaquePanel myUiContainer;
    private final NonOpaquePanel myDockContentUiContainer;

    private DockWindow(String id, Project project, DockContainer container, boolean dialog) {
      super(project, null, dialog);
      myId = id;
      myContainer = container;
      setProject(project);

      if (!(container instanceof DockContainer.Dialog)) {
        setStatusBar(WindowManager.getInstance().getStatusBar(project).createChild());
      }

      myUiContainer = new NonOpaquePanel(new BorderLayout());

      NonOpaquePanel center = new NonOpaquePanel(new BorderLayout(0, 2));
      if (UIUtil.isUnderAquaLookAndFeel()) {
        center.setOpaque(true);
        center.setBackground(JBTabsImpl.MAC_AQUA_BG_COLOR);
      }

      center.add(myNorthPanel, BorderLayout.NORTH);

      myDockContentUiContainer = new NonOpaquePanel(new BorderLayout());
      myDockContentUiContainer.add(myContainer.getContainerComponent(), BorderLayout.CENTER);
      center.add(myDockContentUiContainer, BorderLayout.CENTER);

      myUiContainer.add(center, BorderLayout.CENTER);
      if (myStatusBar != null) {
        myUiContainer.add(myStatusBar.getComponent(), BorderLayout.SOUTH);
      }

      setComponent(myUiContainer);
      addDisposable(container);

      IdeEventQueue.getInstance().addPostprocessor(this, this);

      myContainer.addListener(new DockContainer.Listener.Adapter() {
        @Override
        public void contentRemoved(Object key) {
          getReady().doWhenDone(() -> {
            if (myContainer.isEmpty()) {
              close();
            }
          });
        }
      }, this);

      UISettings.getInstance().addUISettingsListener(source -> updateNorthPanel(), this);

      updateNorthPanel();
    }

    @Override
    public String getId() {
      return myId;
    }

    @Override
    public IdeRootPaneNorthExtension getNorthExtension(String key) {
      return myNorthExtensions.get(key);
    }

    private void updateNorthPanel() {
      if (ApplicationManager.getApplication().isUnitTestMode()) return;
      myNorthPanel.setVisible(
              UISettings.getInstance().SHOW_NAVIGATION_BAR && !(myContainer instanceof DockContainer.Dialog) && !UISettings.getInstance().PRESENTATION_MODE);

      IdeRootPaneNorthExtension[] extensions = IdeRootPaneNorthExtension.EP_NAME.getExtensions(myProject);
      HashSet<String> processedKeys = new HashSet<>();
      for (IdeRootPaneNorthExtension each : extensions) {
        processedKeys.add(each.getKey());
        if (myNorthExtensions.containsKey(each.getKey())) continue;
        IdeRootPaneNorthExtension toInstall = each.copy();
        myNorthExtensions.put(toInstall.getKey(), toInstall);
        myNorthPanel.add(toInstall.getComponent());
      }

      Iterator<String> existing = myNorthExtensions.keySet().iterator();
      while (existing.hasNext()) {
        String each = existing.next();
        if (processedKeys.contains(each)) continue;

        IdeRootPaneNorthExtension toRemove = myNorthExtensions.get(each);
        myNorthPanel.remove(toRemove.getComponent());
        existing.remove();
        Disposer.dispose(toRemove);
      }

      myNorthPanel.revalidate();
      myNorthPanel.repaint();
    }

    public void setTransparent(boolean transparent) {
      if (transparent) {
        WindowManagerEx.getInstanceEx().setAlphaModeEnabled(getFrame(), true);
        WindowManagerEx.getInstanceEx().setAlphaModeRatio(getFrame(), 0.5f);
      }
      else {
        WindowManagerEx.getInstanceEx().setAlphaModeEnabled(getFrame(), true);
        WindowManagerEx.getInstanceEx().setAlphaModeRatio(getFrame(), 0f);
      }
    }

    @Override
    public void dispose() {
      super.dispose();
      myWindows.remove(myContainer);

      for (IdeRootPaneNorthExtension each : myNorthExtensions.values()) {
        Disposer.dispose(each);
      }
      myNorthExtensions.clear();
    }

    @Override
    public boolean dispatch(AWTEvent e) {
      if (e instanceof KeyEvent) {
        if (myCurrentDragSession != null) {
          stopCurrentDragSession();
        }
      }
      return false;
    }

    @Override
    protected JFrame createJFrame(IdeFrame parent) {
      JFrame frame = super.createJFrame(parent);
      installListeners(frame);

      return frame;
    }

    @Override
    protected JDialog createJDialog(IdeFrame parent) {
      JDialog frame = super.createJDialog(parent);
      installListeners(frame);

      return frame;
    }

    private void installListeners(Window frame) {
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          myContainer.closeAll();
        }
      });

      UiNotifyConnector connector = new UiNotifyConnector(((RootPaneContainer)frame).getContentPane(), myContainer);
      Disposer.register(myContainer, connector);
    }
  }
}
