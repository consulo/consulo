/*
 * Copyright 2013-2018 consulo.io
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
package consulo.ide.impl.fileEditor;

import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorWithProvider;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.StaticPosition;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ComponentContainer;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TabbedLayoutStyle;
import consulo.ui.layout.VerticalLayout;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.List;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class UnifiedFileEditorWithProviderComposite implements FileEditorWithProviderComposite {
  private VirtualFile myFile;
  private FileEditor[] myEditors;
  private FileEditorProvider[] myProviders;
  private FileEditorManagerEx myFileEditorManager;

  private final Component[] myComponents;
  private final VerticalLayout[] myTopLayouts;
  /**
   * The editors of a file with several of them are shown as tabs below them, one tab per editor; null for a single editor.
   */
  private final @Nullable TabbedLayout myTabbedLayout;
  private final Tab[] myTabs;
  private final Component myRootComponent;
  private int mySelectedIndex;

  public UnifiedFileEditorWithProviderComposite(VirtualFile file, FileEditor[] editors, FileEditorProvider[] providers, FileEditorManagerEx fileEditorManager) {
    myFile = file;
    myEditors = editors;
    myProviders = providers;
    myFileEditorManager = fileEditorManager;

    // said here rather than left to the index out of bounds every accessor of this class would throw later -
    // a composite of no editors cannot show anything, and the file it was built for is the only clue as to why
    if (editors.length == 0) {
      throw new IllegalArgumentException("No file editor was created for " + file.getPath());
    }

    myComponents = new Component[editors.length];
    myTopLayouts = new VerticalLayout[editors.length];
    for (int i = 0; i < editors.length; i++) {
      FileEditor editor = editors[i];

      Component component = editor.getUIComponent();
      if (component == null) {
        // an editor of the awt frontend only - it has nothing to put in a tab of this window, and a null in
        // here surfaces much later as a tab which simply stays blank
        throw new IllegalArgumentException(
          "File editor " + editor.getClass().getName() + " of " + file.getPath() + " has no unified component");
      }

      myTopLayouts[i] = VerticalLayout.create(Space.NONE);
      myComponents[i] = DockLayout.create(Space.NONE).top(myTopLayouts[i]).center(component);
    }

    if (editors.length > 1) {
      TabbedLayout tabbedLayout = TabbedLayout.create(StaticPosition.BOTTOM);
      tabbedLayout.addStyle(TabbedLayoutStyle.NO_PADDING);
      myTabs = new Tab[editors.length];
      for (int i = 0; i < editors.length; i++) {
        myTabs[i] = tabbedLayout.addTab(editors[i].getName(), myComponents[i]);
      }
      myTabs[0].select();
      tabbedLayout.addSelectListener(event -> onTabSelected(ArrayUtil.indexOf(myTabs, event.getTab())));
      myTabbedLayout = tabbedLayout;
      myRootComponent = tabbedLayout;
    }
    else {
      myTabbedLayout = null;
      myTabs = new Tab[0];
      myRootComponent = myComponents[0];
    }
  }

  /**
   * Another editor of the file was chosen: the editors are told, and so is everybody listening to the selection of editors.
   */
  @RequiredUIAccess
  private void onTabSelected(int index) {
    if (index < 0 || index == mySelectedIndex) {
      return;
    }

    FileEditor oldEditor = myEditors[mySelectedIndex];
    FileEditor newEditor = myEditors[index];
    FileEditorProvider oldProvider = myProviders[mySelectedIndex];
    FileEditorProvider newProvider = myProviders[index];
    mySelectedIndex = index;

    oldEditor.deselectNotify();
    newEditor.selectNotify();

    FileEditorManagerEvent event =
      new FileEditorManagerEvent(myFileEditorManager, myFile, oldEditor, oldProvider, myFile, newEditor, newProvider);
    myFileEditorManager.getProject().getMessageBus().syncPublisher(FileEditorManagerListener.class).selectionChanged(event);
  }

  
  @Override
  public FileEditorProvider[] getProviders() {
    return myProviders;
  }

  @Override
  public void addEditor(FileEditor editor, FileEditorProvider provider) {

  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return null;
  }

  @Override
  public FileEditorWithProvider getSelectedEditorWithProvider() {
    return new FileEditorWithProvider(myEditors[mySelectedIndex], myProviders[mySelectedIndex]);
  }

  @Override
  public VirtualFile getFile() {
    return myFile;
  }

  
  @Override
  public FileEditor[] getEditors() {
    return myEditors;
  }

  
  @Override
  public FileEditor getSelectedEditor() {
    return myEditors[mySelectedIndex];
  }

  @Override
  @RequiredUIAccess
  public void setSelectedEditor(int index) {
    if (myTabbedLayout == null || index < 0 || index >= myTabs.length) {
      return;
    }
    // the tab layout reports the selection back through its listener
    myTabs[index].select();
  }

  @Override
  public List<JComponent> getTopComponents(FileEditor editor) {
    return List.of();
  }

  @Override
  @RequiredUIAccess
  public Disposable addTopComponent(FileEditor editor, ComponentContainer component) {
    int index = ArrayUtil.indexOf(myEditors, editor);
    if (index == -1) {
      throw new IllegalArgumentException("File editor " + editor.getClass().getName() + " is not part of composite of " + myFile.getPath());
    }

    VerticalLayout topLayout = myTopLayouts[index];
    Component uiComponent = component.getUIComponent();
    uiComponent.borderBuilder().bottomSet().apply();
    topLayout.add(uiComponent);
    return () -> topLayout.remove(uiComponent);
  }

  @Override
  public boolean isPinned() {
    return false;
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public void dispose() {

  }

  @Override
  public Component getUIComponent() {
    return myRootComponent;
  }
}
