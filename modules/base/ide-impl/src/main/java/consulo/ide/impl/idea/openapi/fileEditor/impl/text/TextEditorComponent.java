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
package consulo.ide.impl.idea.openapi.fileEditor.impl.text;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.impl.CodeEditorBase;
import consulo.component.messagebus.MessageBusConnection;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.document.util.FileContentUtilCore;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.impl.internal.EditorHistoryManagerImpl;
import consulo.fileEditor.text.TextEditorProvider;
import consulo.ide.impl.fileEditor.text.TextEditorComponentContainer;
import consulo.ide.impl.fileEditor.text.TextEditorComponentContainerFactory;
import consulo.language.editor.impl.internal.markup.EditorMarkupModel;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.IdeActions;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFilePropertyEvent;
import consulo.virtualFileSystem.fileType.FileTypeEvent;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

  public TextEditorComponent(@Nonnull Project project, @Nonnull VirtualFile file, @Nonnull TextEditorImpl textEditor, @Nonnull TextEditorComponentContainerFactory editorFactory) {

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
    myConnection.subscribe(FileTypeListener.class, new MyFileTypeListener());

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
      EditorHistoryManagerImpl.getInstance(myProject).updateHistoryEntry(myFile, false);
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

  @RequiredUIAccess
  private static void assertThread() {
    UIAccess.assertIsUIThread();
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
    Editor editor = EditorFactory.getInstance().createEditor(myDocument, myProject, EditorKind.MAIN_EDITOR);
    ((EditorMarkupModel)editor.getMarkupModel()).setErrorStripeVisible(true);
    ((EditorEx)editor).getGutterComponentEx().setForceShowRightFreePaintersArea(true);

    ((EditorEx)editor).setFile(myFile);

    ((EditorEx)editor).setContextMenuGroupId(IdeActions.GROUP_EDITOR_POPUP);

    ((CodeEditorBase)editor).setDropHandler(new FileDropHandler(editor));

    TextEditorProvider.putTextEditor(editor, myTextEditor);
    return editor;
  }

  private void disposeEditor() {
    EditorFactory.getInstance().releaseEditor(myEditor);
  }

  /**
   * @return whether the editor's document is modified or not
   */
  @RequiredUIAccess
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
    StatusBarEx statusBar = (StatusBarEx)WindowManager.getInstance().getStatusBar(myProject);
    if (statusBar == null) return;
    statusBar.updateWidgets(); // TODO: do we need this?!
  }

  @Nullable
  private Editor validateCurrentEditor() {
    return myTextEditorComponentContainer.validateEditor(myEditor);
  }

  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    Editor e = validateCurrentEditor();
    if (e == null || e.isDisposed()) return null;

    // There's no FileEditorManager for default project (which is used in diff command-line application)
    if (!myProject.isDisposed() && !myProject.isDefault()) {
      Object o = FileEditorManager.getInstance(myProject).getData(dataId, e, e.getCaretModel().getCurrentCaret());
      if (o != null) return o;
    }

    if (Editor.KEY == dataId) {
      return e;
    }
    if (VirtualFile.KEY == dataId) {
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
        myProject.getApplication().invokeLater(myUpdateRunnable);
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
    @RequiredUIAccess
    public void fileTypesChanged(@Nonnull FileTypeEvent event) {
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
    public void propertyChanged(@Nonnull VirtualFilePropertyEvent e) {
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
    @RequiredUIAccess
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
