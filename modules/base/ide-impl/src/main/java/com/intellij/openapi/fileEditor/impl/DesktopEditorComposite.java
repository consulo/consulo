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
package com.intellij.openapi.fileEditor.impl;

import consulo.ide.impl.TabFactoryBuilderImpl;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataProvider;
import consulo.ui.ex.action.IdeActions;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import consulo.application.ApplicationManager;
import consulo.undoRedo.CommandProcessor;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager;
import consulo.fileEditor.FileEditorWithProvider;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import consulo.project.DumbService;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NullUtils;
import consulo.component.util.Weighted;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.util.FocusWatcher;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.ui.ex.PrevNextActionsDescriptor;
import consulo.ui.ex.awt.SideBorder;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import com.intellij.ui.components.JBPanelWithEmptyText;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.Wrapper;
import com.intellij.ui.tabs.UiDecorator;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ObjectUtils;
import consulo.util.collection.SmartList;
import com.intellij.util.containers.ContainerUtil;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.annotation.DeprecationInfo;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorComposite;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.style.StandardColors;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * This class hides internal structure of UI component which represent
 * set of opened editors. For example, one myEditor is represented by its
 * component, more then one myEditor is wrapped into tabbed pane.
 *
 * @author Vladimir Kondratyev
 */
@Deprecated
@DeprecationInfo("Only desktop")
public abstract class DesktopEditorComposite implements FileEditorComposite {
  private static final Logger LOG = Logger.getInstance(DesktopEditorComposite.class);

  /**
   * File for which composite is created
   */
  @Nonnull
  private final VirtualFile myFile;
  /**
   * Whether the composite is pinned or not
   */
  private boolean myPinned;
  /**
   * Editors which are opened in the composite
   */
  protected FileEditor[] myEditors;
  /**
   * This is initial timestamp of the file. It uses to implement
   * "close non modified editors first" feature.
   */
  private final long myInitialFileTimeStamp;
  TabbedPaneWrapper myTabbedPaneWrapper;
  private final MyComponent myComponent;
  private final FocusWatcher myFocusWatcher;
  /**
   * Currently selected myEditor
   */
  private FileEditor mySelectedEditor;
  private final FileEditorManagerEx myFileEditorManager;
  private final Map<FileEditor, JComponent> myTopComponents = new HashMap<>();
  private final Map<FileEditor, JComponent> myBottomComponents = new HashMap<>();
  private final Map<FileEditor, String> myDisplayNames = ContainerUtil.newHashMap();

  /**
   * @param file    {@code file} for which composite is being constructed
   * @param editors {@code edittors} that should be placed into the composite
   * @throws IllegalArgumentException if {@code editors}
   *                                  is {@code null} or {@code providers} is {@code null} or {@code myEditor} arrays is empty
   */
  DesktopEditorComposite(@Nonnull final VirtualFile file, @Nonnull final FileEditor[] editors, @Nonnull final FileEditorManagerEx fileEditorManager) {
    myFile = file;
    myEditors = editors;
    if (NullUtils.hasNull(editors)) throw new IllegalArgumentException("Must not pass null editors in " + Arrays.asList(editors));
    myFileEditorManager = fileEditorManager;
    myInitialFileTimeStamp = myFile.getTimeStamp();

    Disposer.register(fileEditorManager.getProject(), this);

    if (editors.length > 1) {
      myTabbedPaneWrapper = createTabbedPaneWrapper(editors);
      JComponent component = myTabbedPaneWrapper.getComponent();
      myComponent = new MyComponent(component, component);
    }
    else if (editors.length == 1) {
      myTabbedPaneWrapper = null;
      FileEditor editor = editors[0];
      myComponent = new MyComponent(createEditorComponent(editor), editor.getPreferredFocusedComponent());
    }
    else {
      throw new IllegalArgumentException("editors array cannot be empty");
    }

    mySelectedEditor = editors[0];
    myFocusWatcher = new FocusWatcher();
    myFocusWatcher.install(myComponent);

    myFileEditorManager.addFileEditorManagerListener(new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@Nonnull final FileEditorManagerEvent event) {
        final VirtualFile oldFile = event.getOldFile();
        final VirtualFile newFile = event.getNewFile();
        if (Comparing.equal(oldFile, newFile) && Comparing.equal(getFile(), newFile)) {
          Runnable runnable = () -> {
            final FileEditor oldEditor = event.getOldEditor();
            if (oldEditor != null) oldEditor.deselectNotify();
            final FileEditor newEditor = event.getNewEditor();
            if (newEditor != null) newEditor.selectNotify();
            ((FileEditorProviderManagerImpl)FileEditorProviderManager.getInstance()).providerSelected(DesktopEditorComposite.this);
            ((IdeDocumentHistoryImpl)IdeDocumentHistory.getInstance(myFileEditorManager.getProject())).onSelectionChanged();
          };
          if (ApplicationManager.getApplication().isDispatchThread()) {
            CommandProcessor.getInstance().executeCommand(myFileEditorManager.getProject(), runnable, "Switch Active Editor", null);
          }
          else {
            runnable.run(); // not invoked by user
          }
        }
      }
    }, this);
  }

  @Nonnull
  private TabFactoryBuilderImpl.AsJBTabs createTabbedPaneWrapper(FileEditor[] editors) {
    PrevNextActionsDescriptor descriptor = new PrevNextActionsDescriptor(IdeActions.ACTION_NEXT_EDITOR_TAB, IdeActions.ACTION_PREVIOUS_EDITOR_TAB);
    final TabFactoryBuilderImpl.AsJBTabs wrapper = new TabFactoryBuilderImpl.AsJBTabs(myFileEditorManager.getProject(), SwingConstants.BOTTOM, descriptor, this);
    wrapper.getTabs().getPresentation()
            .setUiDecorator(() -> new UiDecorator.UiDecoration(null, JBUI.insets(0, 8)));
    wrapper.getTabs().getComponent().setBorder(new EmptyBorder(0, 0, 1, 0));

    boolean firstEditor = true;
    for (FileEditor editor : editors) {
      JComponent component = firstEditor && myComponent != null ? (JComponent)myComponent.getComponent(0) : createEditorComponent(editor);
      wrapper.addTab(getDisplayName(editor), component);
      firstEditor = false;
    }
    wrapper.addChangeListener(new MyChangeListener());

    return wrapper;
  }

  private JComponent createEditorComponent(final FileEditor editor) {
    JPanel component = new JPanel(new BorderLayout());
    JComponent comp = editor.getComponent();
    if (!FileEditorManagerImpl.isDumbAware(editor)) {
      comp = DumbService.getInstance(myFileEditorManager.getProject()).wrapGently(comp, editor);
    }

    component.add(comp, BorderLayout.CENTER);

    JPanel topPanel = new TopBottomPanel();
    myTopComponents.put(editor, topPanel);
    component.add(topPanel, BorderLayout.NORTH);

    final JPanel bottomPanel = new TopBottomPanel();
    myBottomComponents.put(editor, bottomPanel);
    component.add(bottomPanel, BorderLayout.SOUTH);

    return component;
  }

  /**
   * @return whether myEditor composite is pinned
   */
  public boolean isPinned() {
    return myPinned;
  }

  /**
   * Sets new "pinned" state
   */
  void setPinned(final boolean pinned) {
    myPinned = pinned;
  }

  private void fireSelectedEditorChanged(final FileEditor oldSelectedEditor, final FileEditor newSelectedEditor) {
    if ((!EventQueue.isDispatchThread() || !myFileEditorManager.isInsideChange()) && !Comparing.equal(oldSelectedEditor, newSelectedEditor)) {
      myFileEditorManager.notifyPublisher(() -> {
        final FileEditorManagerEvent event = new FileEditorManagerEvent(myFileEditorManager, myFile, oldSelectedEditor, myFile, newSelectedEditor);
        final FileEditorManagerListener publisher = myFileEditorManager.getProject().getMessageBus().syncPublisher(FileEditorManagerListener.FILE_EDITOR_MANAGER);
        publisher.selectionChanged(event);
      });
      final JComponent component = newSelectedEditor.getComponent();
      final EditorWindowHolder holder = UIUtil.getParentOfType(EditorWindowHolder.class, component);
      if (holder != null) {
        ((FileEditorManagerImpl)myFileEditorManager).addSelectionRecord(myFile, holder.getEditorWindow());
      }
    }
  }


  /**
   * @return preferred focused component inside myEditor composite. Composite uses FocusWatcher to
   * track focus movement inside the myEditor.
   */
  @Nullable
  public JComponent getPreferredFocusedComponent() {
    if (mySelectedEditor == null) return null;

    final Component component = myFocusWatcher.getFocusedComponent();
    if (!(component instanceof JComponent) || !component.isShowing() || !component.isEnabled() || !component.isFocusable()) {
      return getSelectedEditor().getPreferredFocusedComponent();
    }
    return (JComponent)component;
  }

  /**
   * @return file for which composite was created.
   */
  @Override
  @Nonnull
  public VirtualFile getFile() {
    return myFile;
  }

  public FileEditorManager getFileEditorManager() {
    return myFileEditorManager;
  }

  /**
   * @return initial time stamp of the file (on moment of creation of
   * the composite)
   */
  public long getInitialFileTimeStamp() {
    return myInitialFileTimeStamp;
  }

  /**
   * @return editors which are opened in the composite. <b>Do not modify
   * this array</b>.
   */
  @Override
  @Nonnull
  public FileEditor[] getEditors() {
    return myEditors;
  }

  @Nonnull
  public List<JComponent> getTopComponents(@Nonnull FileEditor editor) {
    return getTopBottomComponents(editor, true);
  }

  @Nonnull
  public List<JComponent> getBottomComponents(@Nonnull FileEditor editor) {
    return getTopBottomComponents(editor, false);
  }

  @Nonnull
  private List<JComponent> getTopBottomComponents(@Nonnull FileEditor editor, boolean top) {
    SmartList<JComponent> result = new SmartList<>();
    JComponent container = top ? myTopComponents.get(editor) : myBottomComponents.get(editor);
    for (Component each : container.getComponents()) {
      if (each instanceof NonOpaquePanel) {
        result.add(((NonOpaquePanel)each).getTargetComponent());
      }
    }
    return Collections.unmodifiableList(result);
  }

  public void addTopComponent(FileEditor editor, JComponent component) {
    manageTopOrBottomComponent(editor, component, true, false);
  }

  public void removeTopComponent(FileEditor editor, JComponent component) {
    manageTopOrBottomComponent(editor, component, true, true);
  }

  public void addBottomComponent(FileEditor editor, JComponent component) {
    manageTopOrBottomComponent(editor, component, false, false);
  }

  public void removeBottomComponent(FileEditor editor, JComponent component) {
    manageTopOrBottomComponent(editor, component, false, true);
  }

  private void manageTopOrBottomComponent(FileEditor editor, JComponent component, boolean top, boolean remove) {
    final JComponent container = top ? myTopComponents.get(editor) : myBottomComponents.get(editor);
    assert container != null;

    if (remove) {
      container.remove(component.getParent());
    }
    else {
      NonOpaquePanel wrapper = new NonOpaquePanel(component);
      wrapper.setBorder(createTopBottomSideBorder(top));
      container.add(wrapper, calcComponentInsertionIndex(component, container));
    }
    container.revalidate();
  }

  private static int calcComponentInsertionIndex(@Nonnull JComponent newComponent, @Nonnull JComponent container) {
    for (int i = 0, max = container.getComponentCount(); i < max; i++) {
      Component childWrapper = container.getComponent(i);
      Component childComponent = childWrapper instanceof Wrapper ? ((Wrapper)childWrapper).getTargetComponent() : childWrapper;
      boolean weighted1 = newComponent instanceof Weighted;
      boolean weighted2 = childComponent instanceof Weighted;
      if (!weighted2) continue;
      if (!weighted1) return i;

      double w1 = ((Weighted)newComponent).getWeight();
      double w2 = ((Weighted)childComponent).getWeight();
      if (w1 < w2) return i;
    }
    return -1;
  }

  public void setDisplayName(@Nonnull FileEditor editor, @Nonnull String name) {
    int index = ContainerUtil.indexOfIdentity(ContainerUtil.immutableList(myEditors), editor);
    assert index != -1;

    myDisplayNames.put(editor, name);
    if (myTabbedPaneWrapper != null) {
      myTabbedPaneWrapper.setTitleAt(index, name);
    }
  }

  @Nonnull
  protected String getDisplayName(@Nonnull FileEditor editor) {
    return ObjectUtils.notNull(myDisplayNames.get(editor), editor.getName());
  }

  /**
   * @return currently selected myEditor.
   */
  @Nonnull
  public FileEditor getSelectedEditor() {
    return getSelectedEditorWithProvider().getFileEditor();
  }

  public boolean isDisposed() {
    return myTabbedPaneWrapper != null && myTabbedPaneWrapper.isDisposed();
  }

  /**
   * @return currently selected myEditor with its provider.
   */
  @Override
  @Nonnull
  public abstract FileEditorWithProvider getSelectedEditorWithProvider();

  @Override
  public void setSelectedEditor(final int index) {
    if (myEditors.length == 1) {
      // nothing to do
      LOG.assertTrue(myTabbedPaneWrapper == null);
    }
    else {
      LOG.assertTrue(myTabbedPaneWrapper != null);
      myTabbedPaneWrapper.setSelectedIndex(index);
    }
  }

  /**
   * @return component which represents set of file editors in the UI
   */
  public JComponent getComponent() {
    return myComponent;
  }

  /**
   * @return {@code true} if the composite contains at least one
   * modified myEditor
   */
  public boolean isModified() {
    for (int i = myEditors.length - 1; i >= 0; i--) {
      if (myEditors[i].isModified()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Handles changes of selected myEditor
   */
  private final class MyChangeListener implements ChangeListener {
    @Override
    public void stateChanged(ChangeEvent e) {
      FileEditor oldSelectedEditor = mySelectedEditor;
      LOG.assertTrue(oldSelectedEditor != null);
      int selectedIndex = myTabbedPaneWrapper.getSelectedIndex();
      LOG.assertTrue(selectedIndex != -1);
      mySelectedEditor = myEditors[selectedIndex];
      fireSelectedEditorChanged(oldSelectedEditor, mySelectedEditor);
    }
  }

  private class MyComponent extends JPanel implements DataProvider {
    @Nullable
    private JComponent myFocusComponent;

    public MyComponent(@Nonnull JComponent realComponent, @Nullable JComponent focusComponent) {
      super(new BorderLayout());
      myFocusComponent = focusComponent;
      add(realComponent, BorderLayout.CENTER);
    }

    void setComponent(JComponent newComponent) {
      add(newComponent, BorderLayout.CENTER);
      myFocusComponent = newComponent;
    }

    @Override
    public boolean requestFocusInWindow() {
      return myFocusComponent != null && myFocusComponent.requestFocusInWindow();
    }

    @Override
    public void requestFocus() {
      if (myFocusComponent != null) {
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> {
          IdeFocusManager.getGlobalInstance().requestFocus(myFocusComponent, true);
        });
      }
    }

    @Override
    public boolean requestDefaultFocus() {
      return myFocusComponent != null && myFocusComponent.requestDefaultFocus();
    }

    @Override
    public final Object getData(@Nonnull Key<?> dataId) {
      if (PlatformDataKeys.FILE_EDITOR == dataId) {
        return getSelectedEditor();
      }
      else if (CommonDataKeys.VIRTUAL_FILE == dataId) {
        return myFile.isValid() ? myFile : null;
      }
      else if (CommonDataKeys.VIRTUAL_FILE_ARRAY == dataId) {
        return myFile.isValid() ? new VirtualFile[]{myFile} : null;
      }
      else {
        JComponent component = getPreferredFocusedComponent();
        if (component instanceof DataProvider && component != this) {
          return ((DataProvider)component).getData(dataId);
        }
        return null;
      }
    }
  }

  @Override
  public void dispose() {
    for (FileEditor editor : myEditors) {
      if (!Disposer.isDisposed(editor)) {
        Disposer.dispose(editor);
      }
    }
    myFocusWatcher.deinstall(myFocusWatcher.getTopComponent());
  }

  @RequiredUIAccess
  void addEditor(@Nonnull FileEditor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myEditors = ArrayUtil.append(myEditors, editor);
    if (myTabbedPaneWrapper == null) {
      myTabbedPaneWrapper = createTabbedPaneWrapper(myEditors);
      myComponent.setComponent(myTabbedPaneWrapper.getComponent());
    }
    else {
      JComponent component = createEditorComponent(editor);
      myTabbedPaneWrapper.addTab(getDisplayName(editor), component);
    }
    myFocusWatcher.deinstall(myFocusWatcher.getTopComponent());
    myFocusWatcher.install(myComponent);
  }

  private static class TopBottomPanel extends JBPanelWithEmptyText {
    private TopBottomPanel() {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    @Override
    public Color getBackground() {
      ColorValue color = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.GUTTER_BACKGROUND);
      return TargetAWT.to(color == null ? EditorColors.GUTTER_BACKGROUND.getDefaultColorValue() : color);
    }
  }

  @Nonnull
  private static SideBorder createTopBottomSideBorder(boolean top) {
    return new SideBorder(null, top ? SideBorder.BOTTOM : SideBorder.TOP) {
      @Override
      public Color getLineColor() {
        ColorValue result = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.TEARLINE_COLOR);
        return TargetAWT.to(result == null ? StandardColors.BLACK : result);
      }
    };
  }
}
