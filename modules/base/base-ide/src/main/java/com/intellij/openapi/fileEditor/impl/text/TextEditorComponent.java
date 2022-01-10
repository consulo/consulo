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
package com.intellij.openapi.fileEditor.impl.text;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentAdapter;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorMarkupModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFilePropertyEvent;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.StatusBarEx;
import com.intellij.util.FileContentUtilCore;
import com.intellij.util.messages.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.editor.internal.EditorInternal;
import consulo.fileEditor.impl.text.TextEditorComponentContainer;
import consulo.fileEditor.impl.text.TextEditorComponentContainerFactory;
import consulo.fileEditor.impl.text.TextEditorProvider;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class TextEditorComponent implements DataProvider, Disposable {
  private static final Logger LOG = Logger.getInstance(TextEditorComponent.class);

  private final Project myProject;
  @Nonnull
  private final VirtualFile myFile;

  private final TextEditorImpl myTextEditor;
  /**
   * Document to be edited
   */
  private final Document myDocument;

  @Nonnull
  private final Editor myEditor;

  /**
   * Whether the editor's document is modified or not
   */
  private boolean myModified;
  /**
   * Whether the editor is valid or not
   */
  private boolean myValid;

  private final EditorHighlighterUpdater myEditorHighlighterUpdater;

  private final TextEditorComponentContainer myTextEditorComponentContainer;

  public TextEditorComponent(@Nonnull final Project project, @Nonnull final VirtualFile file, @Nonnull final TextEditorImpl textEditor, @Nonnull TextEditorComponentContainerFactory editorFactory) {

    myProject = project;
    myFile = file;
    myTextEditor = textEditor;

    myDocument = FileDocumentManager.getInstance().getDocument(myFile);
    LOG.assertTrue(myDocument != null);
    myDocument.addDocumentListener(new MyDocumentListener(), this);

    myEditor = createEditor();
    myModified = isModifiedImpl();
    myValid = isEditorValidImpl();
    LOG.assertTrue(myValid);

    myTextEditorComponentContainer = editorFactory.createTextComponentContainer(myEditor, this, this);

    MyVirtualFileListener myVirtualFileListener = new MyVirtualFileListener();
    myFile.getFileSystem().addVirtualFileListener(myVirtualFileListener);
    Disposer.register(this, () -> myFile.getFileSystem().removeVirtualFileListener(myVirtualFileListener));
    MessageBusConnection myConnection = project.getMessageBus().connect(this);
    myConnection.subscribe(FileTypeManager.TOPIC, new MyFileTypeListener());

    myEditorHighlighterUpdater = new EditorHighlighterUpdater(myProject, this, (EditorEx)myEditor, myFile);
  }

  private volatile boolean myDisposed;

  /**
   * Disposes all resources allocated be the TextEditorComponent. It disposes all created
   * editors, unregisters listeners. The behaviour of the splitter after disposing is
   * unpredictable.
   */
  @Override
  public void dispose() {
    if (!myProject.isDefault()) { // There's no EditorHistoryManager for default project (which is used in diff command-line application)
      EditorHistoryManager.getInstance(myProject).updateHistoryEntry(myFile, false);
    }
    disposeEditor();

    myDisposed = true;
    //myFocusWatcher.deinstall(this);
    //removePropertyChangeListener(mySplitterPropertyChangeListener);

    //super.dispose();
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  /**
   * Should be invoked when the corresponding {@code TextEditorImpl}
   * is selected. Updates the status bar.
   */
  void selectNotify() {
    updateStatusBar();
  }

  private static void assertThread() {
    ApplicationManager.getApplication().assertIsDispatchThread();
  }

  /**
   * @return most recently used editor. This method never returns {@code null}.
   */
  @Nonnull
  public Editor getEditor() {
    return myEditor;
  }

  @Nonnull
  private Editor createEditor() {
    Editor editor = EditorFactory.getInstance().createEditor(myDocument, myProject);
    ((EditorMarkupModel)editor.getMarkupModel()).setErrorStripeVisible(true);
    ((EditorEx)editor).getGutterComponentEx().setForceShowRightFreePaintersArea(true);

    ((EditorEx)editor).setFile(myFile);

    ((EditorEx)editor).setContextMenuGroupId(IdeActions.GROUP_EDITOR_POPUP);

    ((EditorInternal)editor).setDropHandler(new FileDropHandler(editor));

    TextEditorProvider.putTextEditor(editor, myTextEditor);
    return editor;
  }

  private void disposeEditor() {
    EditorFactory.getInstance().releaseEditor(myEditor);
  }

  /**
   * @return whether the editor's document is modified or not
   */
  boolean isModified() {
    assertThread();
    return myModified;
  }

  /**
   * Just calculates "modified" property
   */
  private boolean isModifiedImpl() {
    return FileDocumentManager.getInstance().isFileModified(myFile);
  }

  /**
   * Updates "modified" property and fires event if necessary
   */
  void updateModifiedProperty() {
    Boolean oldModified = myModified;
    myModified = isModifiedImpl();
    myTextEditor.firePropertyChange(FileEditor.PROP_MODIFIED, oldModified, myModified);
  }

  /**
   * Name {@code isValid} is in use in {@code java.awt.Component}
   * so we change the name of method to {@code isEditorValid}
   *
   * @return whether the editor is valid or not
   */
  boolean isEditorValid() {
    return myValid && !myEditor.isDisposed();
  }

  /**
   * Just calculates
   */
  private boolean isEditorValidImpl() {
    return FileDocumentManager.getInstance().getDocument(myFile) != null;
  }

  private void updateValidProperty() {
    Boolean oldValid = myValid;
    myValid = isEditorValidImpl();
    myTextEditor.firePropertyChange(FileEditor.PROP_VALID, oldValid, myValid);
  }

  /**
   * Updates frame's status bar: insert/overwrite mode, caret position
   */
  private void updateStatusBar() {
    final StatusBarEx statusBar = (StatusBarEx)WindowManager.getInstance().getStatusBar(myProject);
    if (statusBar == null) return;
    statusBar.updateWidgets(); // TODO: do we need this?!
  }

  @Nullable
  private Editor validateCurrentEditor() {
    return myTextEditorComponentContainer.validateEditor(myEditor);
  }

  @Override
  public Object getData(@Nonnull final Key<?> dataId) {
    final Editor e = validateCurrentEditor();
    if (e == null || e.isDisposed()) return null;

    // There's no FileEditorManager for default project (which is used in diff command-line application)
    if (!myProject.isDisposed() && !myProject.isDefault()) {
      final Object o = FileEditorManager.getInstance(myProject).getData(dataId, e, e.getCaretModel().getCurrentCaret());
      if (o != null) return o;
    }

    if (CommonDataKeys.EDITOR == dataId) {
      return e;
    }
    if (CommonDataKeys.VIRTUAL_FILE == dataId) {
      return myFile.isValid() ? myFile : null;  // fix for SCR 40329
    }
    return null;
  }

  /**
   * Updates "modified" property
   */
  private final class MyDocumentListener extends DocumentAdapter {
    /**
     * We can reuse this runnable to decrease number of allocated object.
     */
    private final Runnable myUpdateRunnable;
    private boolean myUpdateScheduled;

    public MyDocumentListener() {
      myUpdateRunnable = () -> {
        myUpdateScheduled = false;
        updateModifiedProperty();
      };
    }

    @Override
    public void documentChanged(DocumentEvent e) {
      if (!myUpdateScheduled) {
        // document's timestamp is changed later on undo or PSI changes
        ApplicationManager.getApplication().invokeLater(myUpdateRunnable);
        myUpdateScheduled = true;
      }
    }
  }

  /**
   * Listen changes of file types. When type of the file changes we need
   * to also change highlighter.
   */
  private final class MyFileTypeListener implements FileTypeListener {
    @Override
    public void fileTypesChanged(@Nonnull final FileTypeEvent event) {
      assertThread();
      // File can be invalid after file type changing. The editor should be removed
      // by the FileEditorManager if it's invalid.
      updateValidProperty();
    }
  }

  /**
   * Updates "valid" property and highlighters (if necessary)
   */
  private final class MyVirtualFileListener implements VirtualFileListener {
    @Override
    public void propertyChanged(@Nonnull final VirtualFilePropertyEvent e) {
      if (VirtualFile.PROP_NAME.equals(e.getPropertyName())) {
        // File can be invalidated after file changes name (extension also
        // can changes). The editor should be removed if it's invalid.
        updateValidProperty();
        if (Comparing.equal(e.getFile(), myFile) &&
            (FileContentUtilCore.FORCE_RELOAD_REQUESTOR.equals(e.getRequestor()) || !Comparing.equal(e.getOldValue(), e.getNewValue()))) {
          myEditorHighlighterUpdater.updateHighlighters();
        }
      }
    }

    @Override
    public void contentsChanged(@Nonnull VirtualFileEvent event) {
      if (event.isFromSave()) { // commit
        assertThread();
        VirtualFile file = event.getFile();
        LOG.assertTrue(file.isValid());
        if (myFile.equals(file)) {
          updateModifiedProperty();
        }
      }
    }
  }

  public void loadingFinished() {
    myTextEditorComponentContainer.loadingFinished();
  }

  @Nonnull
  public TextEditorComponentContainer getComponentContainer() {
    return myTextEditorComponentContainer;
  }

  @Nonnull
  public VirtualFile getFile() {
    return myFile;
  }
}
