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
package consulo.desktop.awt.fileEditor.impl;

import consulo.application.ReadAction;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.component.ProcessCanceledException;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.impl.internal.AsyncConfigTreeReader;
import consulo.desktop.awt.uiOld.AWTComponentProviderUtil;
import consulo.fileEditor.impl.internal.FileEditorsSplittersBase;
import consulo.ide.impl.idea.diagnostic.Activity;
import consulo.ide.impl.idea.diagnostic.ActivityCategory;
import consulo.ide.impl.idea.diagnostic.StartUpMeasurer;
import consulo.fileEditor.impl.internal.FileEditorHistoryUtil;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.fileEditor.impl.internal.FileEditorOpenOptions;
import consulo.fileEditor.impl.internal.HistoryEntry;
import consulo.ide.impl.idea.openapi.fileEditor.impl.text.FileDropHandler;
import consulo.ide.impl.idea.openapi.wm.impl.IdePanePanel;
import consulo.ui.ex.awt.tab.JBTabs;
import consulo.ide.impl.idea.ui.tabs.impl.JBTabsImpl;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.dock.DockManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.IdeFocusTraversalPolicy;
import consulo.ui.ex.awt.OnePixelSplitter;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.FocusWatcher;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.event.KeymapManagerListener;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.style.StyleManager;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ContainerEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DesktopFileEditorsSplitters extends FileEditorsSplittersBase<DesktopFileEditorWindow> {
  private static final Logger LOG = Logger.getInstance(DesktopFileEditorsSplitters.class);

  private static final Key<Object> DUMMY_KEY = Key.create("EditorsSplitters.dummy.key");

  private final MyFocusWatcher myFocusWatcher;
  private final UIBuilder myUIBuilder = new UIBuilder();

  private final IdePanePanel myComponent;

  public DesktopFileEditorsSplitters(Project project,
                                     ApplicationConcurrency applicationConcurrency,
                                     FileEditorManagerImpl manager,
                                     DockManager dockManager,
                                     boolean createOwnDockableContainer) {
    super(applicationConcurrency, project, manager);

    myComponent = new IdePanePanel(new BorderLayout()) {
      @Override
      protected void paintComponent(Graphics g) {
        if (showEmptyText()) {
          super.paintComponent(g);
          g.setColor(
            StyleManager.get().getCurrentStyle().isDark()
              ? UIUtil.getBorderColor()
              : new Color(0, 0, 0, 50)
          );
          g.drawLine(0, 0, getWidth(), 0);

          EditorEmptyTextPainter.ourInstance.paintEmptyText(this, g);
        }
      }
    };
    myComponent.setFocusTraversalPolicy(new MyFocusTraversalPolicy());
    myComponent.setTransferHandler(new MyTransferHandler());
    DataManager.registerDataProvider(myComponent, dataId -> {
      if (dataId == KEY) {
        return this;
      }
      return null;
    });

    AWTComponentProviderUtil.putMark(myComponent, this);

    myFocusWatcher = new MyFocusWatcher();

    clear();

    if (createOwnDockableContainer) {
      DesktopDockableEditorTabbedContainer dockable =
        new DesktopDockableEditorTabbedContainer(myManager.getProject(), this, false);
      Disposer.register(manager.getProject(), dockable);
      dockManager.register(dockable);
    }
    KeymapManagerListener keymapListener = keymap -> {
      myComponent.invalidate();
      myComponent.repaint();
    };
    KeymapManager.getInstance().addKeymapManagerListener(keymapListener, this);
  }

  @Override
  public void revalidate() {
    myComponent.revalidate();
  }

  @Nonnull
  @Override
  protected DesktopFileEditorWindow[] createArray(int size) {
    return new DesktopFileEditorWindow[size];
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return myComponent;
  }

  @Override
  public void clear() {
    for (DesktopFileEditorWindow window : myWindows) {
      window.dispose();
    }
    myComponent.removeAll();
    myWindows.clear();
    setCurrentWindow(null);
    myComponent.repaint(); // revalidate doesn't repaint correctly after "Close All"
  }

  @Override
  public void startListeningFocus() {
    myFocusWatcher.install(myComponent);
  }

  @Override
  protected void stopListeningFocus() {
    myFocusWatcher.deinstall(myComponent);
  }

  @Override
  public void writeExternal(@Nonnull Element element) {
    if (myComponent.getComponentCount() == 0) {
      return;
    }

    JPanel panel = (JPanel)myComponent.getComponent(0);
    if (panel.getComponentCount() != 0) {
      try {
        element.addContent(writePanel(panel.getComponent(0)));
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
  }

  @SuppressWarnings("HardCodedStringLiteral")
  private Element writePanel(@Nonnull Component comp) {
    if (comp instanceof Splitter splitter) {
      final Element res = new Element("splitter");
      res.setAttribute("split-orientation", splitter.getOrientation() ? "vertical" : "horizontal");
      res.setAttribute("split-proportion", Float.toString(splitter.getProportion()));
      final Element first = new Element("split-first");
      first.addContent(writePanel(splitter.getFirstComponent().getComponent(0)));
      final Element second = new Element("split-second");
      second.addContent(writePanel(splitter.getSecondComponent().getComponent(0)));
      res.addContent(first);
      res.addContent(second);
      return res;
    }
    else if (comp instanceof JBTabs jbTabs) {
      final Element res = new Element("leaf");
      Integer limit = UIUtil.getClientProperty(jbTabs.getComponent(), JBTabsImpl.SIDE_TABS_SIZE_LIMIT_KEY);
      if (limit != null) {
        res.setAttribute(JBTabsImpl.SIDE_TABS_SIZE_LIMIT_KEY.toString(), String.valueOf(limit));
      }

      writeWindow(res, findWindowWith(comp));
      return res;
    }
    else {
      LOG.error(comp.getClass().getName());
      return null;
    }
  }

  private void writeWindow(@Nonnull Element res, @Nullable DesktopFileEditorWindow window) {
    if (window != null) {
      FileEditorWithProviderComposite[] composites = window.getEditors();
      for (int i = 0; i < composites.length; i++) {
        VirtualFile file = window.getFileAt(i);
        res.addContent(writeComposite(file, composites[i], window.isFilePinned(file), window.getSelectedEditor()));
      }
    }
  }

  @Nonnull
  private Element writeComposite(
    VirtualFile file,
    FileEditorWithProviderComposite composite,
    boolean pinned,
    FileEditorWithProviderComposite selectedEditor
  ) {
    Element fileElement = new Element("file");
    fileElement.setAttribute("leaf-file-name", file.getName()); // TODO: all files
    FileEditorHistoryUtil.currentStateAsHistoryEntry(composite).writeExternal(fileElement, myProject);
    fileElement.setAttribute(AsyncConfigTreeReader.PINNED, Boolean.toString(pinned));
    fileElement.setAttribute(AsyncConfigTreeReader.CURRENT_IN_TAB, Boolean.toString(composite.equals(selectedEditor)));
    return fileElement;
  }

  @Nonnull
  @Override
  public CompletableFuture<?> openFilesAsync(@Nonnull UIAccess uiAccess) {
    if (mySplittersElement == null) {
      return CompletableFuture.completedFuture(null);
    }

    return myUIBuilder.process(mySplittersElement, getTopPanel(), uiAccess).whenCompleteAsync((comp, throwable) -> {
      if (comp != null) {
        myComponent.removeAll();
        myComponent.add(comp, BorderLayout.CENTER);
        mySplittersElement = null;
      }
      // clear empty splitters
      for (DesktopFileEditorWindow window : getWindows()) {
        if (window.getEditors().length == 0) {
          for (DesktopFileEditorWindow sibling : window.findSiblings()) {
            sibling.unsplit(false);
          }
        }
      }
    }, uiAccess);
  }

  @Override
  public boolean isShowing() {
    return myComponent.isShowing();
  }

  @Override
  public int getSplitCount() {
    if (myComponent.getComponentCount() > 0) {
      JPanel panel = (JPanel)myComponent.getComponent(0);
      return getSplitCount(panel);
    }
    return 0;
  }

  private static int getSplitCount(JComponent component) {
    if (component.getComponentCount() > 0) {
      final JComponent firstChild = (JComponent)component.getComponent(0);
      if (firstChild instanceof Splitter splitter) {
        return getSplitCount(splitter.getFirstComponent()) + getSplitCount(splitter.getSecondComponent());
      }
      return 1;
    }
    return 0;
  }

  @Nullable
  JBTabs getTabsAt(RelativePoint point) {
    Point thisPoint = point.getPoint(myComponent);
    Component c = SwingUtilities.getDeepestComponentAt(myComponent, thisPoint.x, thisPoint.y);
    while (c != null) {
      if (c instanceof JBTabs jbTabs) {
        return jbTabs;
      }
      c = c.getParent();
    }

    return null;
  }

  boolean isEmptyVisible() {
    DesktopFileEditorWindow[] windows = getWindows();
    for (DesktopFileEditorWindow each : windows) {
      if (!each.isEmptyVisible()) {
        return false;
      }
    }
    return true;
  }

  private final class MyFocusTraversalPolicy extends IdeFocusTraversalPolicy {
    @Override
    public final Component getDefaultComponent(final Container focusCycleRoot) {
      if (myCurrentWindow != null) {
        final DesktopFileEditorWithProviderComposite selectedEditor = myCurrentWindow.getSelectedEditor();
        if (selectedEditor != null) {
          return IdeFocusTraversalPolicy.getPreferredFocusedComponent(selectedEditor.getComponent(), this);
        }
      }
      return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myComponent, this);
    }
  }

  @Nullable
  public JPanel getTopPanel() {
    return myComponent.getComponentCount() > 0 ? (JPanel)myComponent.getComponent(0) : null;
  }

  @RequiredUIAccess
  @Override
  protected void createCurrentWindow() {
    LOG.assertTrue(myCurrentWindow == null);
    setCurrentWindow(createEditorWindow());
    myComponent.add(myCurrentWindow.myPanel, BorderLayout.CENTER);
  }

  protected DesktopFileEditorWindow createEditorWindow() {
    return new DesktopFileEditorWindow(this);
  }

  @Override
  @Nonnull
  public DesktopFileEditorWindow[] getOrderedWindows() {
    final List<DesktopFileEditorWindow> res = new ArrayList<>();

    // Collector for windows in tree ordering:
    class Inner {
      private void collect(final JPanel panel) {
        final Component comp = panel.getComponent(0);
        if (comp instanceof Splitter splitter) {
          collect((JPanel)splitter.getFirstComponent());
          collect((JPanel)splitter.getSecondComponent());
        }
        else if (comp instanceof JPanel || comp instanceof JBTabs) {
          final DesktopFileEditorWindow window = findWindowWith(comp);
          if (window != null) {
            res.add(window);
          }
        }
      }
    }

    // get root component and traverse splitters tree:
    if (myComponent.getComponentCount() != 0) {
      final Component comp = myComponent.getComponent(0);
      LOG.assertTrue(comp instanceof JPanel);
      final JPanel panel = (JPanel)comp;
      if (panel.getComponentCount() != 0) {
        new Inner().collect(panel);
      }
    }

    LOG.assertTrue(res.size() == myWindows.size());
    return res.toArray(new DesktopFileEditorWindow[res.size()]);
  }

  @Nullable
  private DesktopFileEditorWindow findWindowWith(final Component component) {
    if (component != null) {
      for (final DesktopFileEditorWindow window : myWindows) {
        if (SwingUtilities.isDescendingFrom(component, window.myPanel)) {
          return window;
        }
      }
    }
    return null;
  }

  @RequiredUIAccess
  @Override
  @Nullable
  public FileEditorWindow openInRightSplit(@Nonnull VirtualFile file, boolean requestFocus) {
    DesktopFileEditorWindow window = getCurrentWindow();

    if (window == null) {
      return null;
    }
    Container parent = window.myPanel.getParent();
    if (parent instanceof Splitter splitter) {
      JComponent component = splitter.getSecondComponent();
      if (component != window.myPanel) {
        //reuse
        FileEditorWindow rightSplitWindow = findWindowWith(component);
        if (rightSplitWindow != null) {
          myManager.openFileWithProviders(file, requestFocus, rightSplitWindow);
          return rightSplitWindow;
        }
      }
    }

    return window.split(SwingConstants.VERTICAL, true, file, requestFocus);
  }

  public boolean isFloating() {
    return false;
  }

  public boolean isPreview() {
    return false;
  }

  private final class MyFocusWatcher extends FocusWatcher {
    @Override
    protected void focusedComponentChanged(final Component component, final AWTEvent cause) {
      DesktopFileEditorWindow newWindow = null;

      if (component != null) {
        newWindow = findWindowWith(component);
      }
      else if (cause instanceof ContainerEvent && cause.getID() == ContainerEvent.COMPONENT_REMOVED) {
        // do not change current window in case of child removal as in JTable.removeEditor
        // otherwise Escape in a toolwindow will not focus editor with JTable content
        return;
      }

      setCurrentWindow(newWindow);
      setCurrentWindow(newWindow, false);
    }
  }

  @Override
  public void toFront() {
    ProjectIdeFocusManager.getInstance(myProject).toFront(getComponent());
  }

  private final class MyTransferHandler extends TransferHandler {
    private final FileDropHandler myFileDropHandler = new FileDropHandler(null);

    @Override
    public boolean importData(JComponent comp, Transferable t) {
      if (myFileDropHandler.canHandleDrop(t.getTransferDataFlavors())) {
        myFileDropHandler.handleDrop(t, myManager.getProject(), myCurrentWindow);
        return true;
      }
      return false;
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
      return myFileDropHandler.canHandleDrop(transferFlavors);
    }
  }


  private class UIBuilder extends AsyncConfigTreeReader<JPanel> {
    @Nonnull
    @Override
    protected CompletableFuture<JPanel> processFiles(@Nonnull List<Element> fileElements,
                                                     final JPanel context,
                                                     Element parent,
                                                     UIAccess uiAccess) {
      return uiAccess.giveAsync(() -> createEditorWindow(context, parent))
                     .thenApplyAsync((window) -> processFilesImpl(fileElements, uiAccess, window));
    }

    private DesktopFileEditorWindow createEditorWindow(JPanel context, Element parent) {
      DesktopFileEditorWindow editorWindow =
        context == null ? DesktopFileEditorsSplitters.this.createEditorWindow() : findWindowWith(context);
      if (editorWindow != null) {
        updateTabSizeLimit(editorWindow, parent.getAttributeValue(JBTabsImpl.SIDE_TABS_SIZE_LIMIT_KEY.toString()));
      }
      return editorWindow;
    }

    private JPanel processFilesImpl(@Nonnull List<Element> fileElements,
                                    UIAccess uiAccess,
                                    DesktopFileEditorWindow window) {
      LOG.assertTrue(window != null);
      VirtualFile focusedFile = null;

      for (int i = 0; i < fileElements.size(); i++) {
        final Element file = fileElements.get(i);
        Element historyElement = file.getChild(HistoryEntry.TAG);
        String fileName = historyElement.getAttributeValue(HistoryEntry.FILE_ATTR);
        Activity activity = StartUpMeasurer.startActivity(PathUtil.getFileName(fileName), ActivityCategory.REOPENING_EDITOR);
        VirtualFile virtualFile = null;
        try {
          final FileEditorManagerImpl fileEditorManager = getManager();
          final HistoryEntry entry = HistoryEntry.createLight(fileEditorManager.getProject(), historyElement);
          virtualFile = entry.getFile();
          if (virtualFile == null) throw new InvalidDataException("No file exists: " + entry.getFilePointer().getUrl());
          virtualFile.putUserData(OPENED_IN_BULK, Boolean.TRUE);
          VirtualFile finalVirtualFile = virtualFile;
          Document document =
            ReadAction.compute(() -> finalVirtualFile.isValid() ? FileDocumentManager.getInstance().getDocument(finalVirtualFile) : null);

          boolean isCurrentTab = Boolean.valueOf(file.getAttributeValue(CURRENT_IN_TAB));
          FileEditorOpenOptions openOptions = new FileEditorOpenOptions().withPin(Boolean.valueOf(file.getAttributeValue(PINNED)))
                                                                         .withIndex(i)
                                                                         .withReopeningEditorsOnStartup();

          fileEditorManager.openFileImpl4(uiAccess, window, virtualFile, entry, openOptions);
          if (isCurrentTab) {
            focusedFile = virtualFile;
          }
          if (document != null) {
            // This is just to make sure document reference is kept on stack till this point
            // so that document is available for folding state deserialization in HistoryEntry constructor
            // and that document will be created only once during file opening
            document.putUserData(DUMMY_KEY, null);
          }
        }
        catch (InvalidDataException e) {
          LOG.warn(e);
        }
        finally {
          if (virtualFile != null) virtualFile.putUserData(OPENED_IN_BULK, null);
        }
        activity.end();
      }
      if (focusedFile != null) {
        getManager().addSelectionRecord(focusedFile, window);
        VirtualFile finalFocusedFile = focusedFile;
        uiAccess.execute(() -> {
          FileEditorWithProviderComposite editor = window.findFileComposite(finalFocusedFile);
          if (editor != null) {
            window.setEditor(editor, true, true);
          }
        });
      }
      else {
        ToolWindowManager manager = ToolWindowManager.getInstance(getManager().getProject());
        uiAccess.execute(() -> {
          if (null == manager.getActiveToolWindowId()) {
            ToolWindow toolWindow = manager.getToolWindow(ToolWindowId.PROJECT_VIEW);
            if (toolWindow != null) toolWindow.activate(null);
          }
        });
      }
      return window.myPanel;
    }


    @Nonnull
    @Override
    protected CompletableFuture<JPanel> processSplitter(@Nonnull Element splitterElement,
                                                        Element firstChild,
                                                        Element secondChild,
                                                        final JPanel context,
                                                        @Nonnull UIAccess uiAccess) {
      return processSplitterImpl(splitterElement, firstChild, secondChild, context, uiAccess);
    }


    protected CompletableFuture<JPanel> processSplitterImpl(@Nonnull Element splitterElement,
                                                            Element firstChild,
                                                            Element secondChild,
                                                            final JPanel context,
                                                            UIAccess uiAccess) {
      if (context == null) {
        final boolean orientation = "vertical".equals(splitterElement.getAttributeValue("split-orientation"));
        final float proportion = Float.valueOf(splitterElement.getAttributeValue("split-proportion"));
        final CompletableFuture<JPanel> firstComponent = process(firstChild, null, uiAccess);
        final CompletableFuture<JPanel> secondComponent = process(secondChild, null, uiAccess);

        return firstComponent.thenCombineAsync(secondComponent, (p1, p2) -> {
          JPanel panel = new JPanel(new BorderLayout());
          panel.setOpaque(false);
          Splitter splitter = new OnePixelSplitter(orientation, proportion, 0.1f, 0.9f);
          panel.add(splitter, BorderLayout.CENTER);
          splitter.setFirstComponent(p1);
          splitter.setSecondComponent(p2);
          return panel;
        }, uiAccess);
      }
      else {
        return uiAccess.giveAsync(() -> {
          if (context.getComponent(0) instanceof Splitter splitter) {
            return Couple.of((JPanel)splitter.getFirstComponent(), (JPanel)splitter.getSecondComponent());
          }
          else {
            return Couple.create(context, context);
          }
        }).thenCompose(couple -> {
          CompletableFuture<JPanel> f1 = process(firstChild, couple.getFirst(), uiAccess);
          CompletableFuture<JPanel> f2 = process(firstChild, couple.getSecond(), uiAccess);

          return f1.thenCombineAsync(f2, (jPanel, jPanel2) -> context, uiAccess);
        });
      }
    }
  }

  private static void updateTabSizeLimit(DesktopFileEditorWindow editorWindow, String tabSizeLimit) {
    DesktopAWTEditorTabbedContainer tabbedPane = editorWindow.getTabbedPane();
    if (tabSizeLimit != null) {
      try {
        int limit = Integer.parseInt(tabSizeLimit);
        UIUtil.invokeAndWaitIfNeeded((Runnable)() -> UIUtil.putClientProperty(tabbedPane.getComponent(),
                                                                              JBTabsImpl.SIDE_TABS_SIZE_LIMIT_KEY,
                                                                              limit));
      }
      catch (NumberFormatException ignored) {
      }
    }
  }
}
