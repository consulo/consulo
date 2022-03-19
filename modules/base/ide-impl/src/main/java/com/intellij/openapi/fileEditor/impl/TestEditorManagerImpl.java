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
package com.intellij.openapi.fileEditor.impl;

import com.intellij.ide.highlighter.HighlighterFactory;
import consulo.disposer.Disposable;
import consulo.fileEditor.*;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.undoRedo.CommandProcessor;
import consulo.fileEditor.FileEditorWithProvider;
import consulo.logging.Logger;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorHighlighter;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.text.TextEditorPsiDataProvider;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerAdapter;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VfsUtilCore;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.util.IncorrectOperationException;
import consulo.fileEditor.FileEditorComposite;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorsSplitters;
import consulo.fileEditor.impl.text.TextEditorProvider;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.UIAccess;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

final class TestEditorManagerImpl extends FileEditorManagerEx implements Disposable {
  private static final Logger LOG = Logger.getInstance(TestEditorManagerImpl.class);

  private final TestEditorSplitter myTestEditorSplitter = new TestEditorSplitter();

  private final Project myProject;
  private int counter = 0;

  private final Map<VirtualFile, Editor> myVirtualFile2Editor = new HashMap<>();
  private VirtualFile myActiveFile;
  private static final LightVirtualFile LIGHT_VIRTUAL_FILE = new LightVirtualFile("Dummy.java");

  public TestEditorManagerImpl(@Nonnull Project project) {
    myProject = project;
    registerExtraEditorDataProvider(new TextEditorPsiDataProvider(), null);

    project.getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerAdapter() {
      @Override
      public void projectClosed(Project project, UIAccess uiAccess) {
        if (project == myProject) {
          closeAllFiles();
        }
      }
    });
  }

  @Override
  @Nonnull
  public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@Nonnull final VirtualFile file,
                                                                        final boolean focusEditor,
                                                                        boolean searchForSplitter) {
    final Ref<Pair<FileEditor[], FileEditorProvider[]>> result = new Ref<>();
    CommandProcessor.getInstance().executeCommand(myProject, () -> result.set(openFileImpl3(file, focusEditor)), "", null);
    return result.get();

  }

  private Pair<FileEditor[], FileEditorProvider[]> openFileImpl3(final VirtualFile file, boolean focusEditor) {
    // for non-text editors. uml, etc
    final FileEditorProvider provider = file.getUserData(FileEditorProvider.KEY);
    if (provider != null && provider.accept(getProject(), file)) {
      return Pair.create(new FileEditor[]{provider.createEditor(getProject(), file)}, new FileEditorProvider[]{provider});
    }

    //text editor
    Editor editor = openTextEditor(new OpenFileDescriptorImpl(myProject, file), focusEditor);
    assert editor != null;
    final FileEditor fileEditor = TextEditorProvider.getInstance().getTextEditor(editor);
    final FileEditorProvider fileEditorProvider = getProvider();
    Pair<FileEditor[], FileEditorProvider[]> result = Pair.create(new FileEditor[]{fileEditor}, new FileEditorProvider[]{fileEditorProvider});

    modifyTabWell(new Runnable() {
      @Override
      public void run() {
        myTestEditorSplitter.openAndFocusTab(file, fileEditor, fileEditorProvider);
      }
    });

    return result;
  }

  private void modifyTabWell(Runnable tabWellModification) {
    FileEditor lastFocusedEditor = myTestEditorSplitter.getFocusedFileEditor();
    VirtualFile lastFocusedFile  = myTestEditorSplitter.getFocusedFile();
    FileEditorProvider oldProvider = myTestEditorSplitter.getProviderFromFocused();

    tabWellModification.run();

    FileEditor currentlyFocusedEditor = myTestEditorSplitter.getFocusedFileEditor();
    VirtualFile currentlyFocusedFile = myTestEditorSplitter.getFocusedFile();
    FileEditorProvider newProvider = myTestEditorSplitter.getProviderFromFocused();

    final FileEditorManagerEvent event =
            new FileEditorManagerEvent(this, lastFocusedFile, lastFocusedEditor, oldProvider, currentlyFocusedFile, currentlyFocusedEditor, newProvider);
    final FileEditorManagerListener publisher = getProject().getMessageBus().syncPublisher(FileEditorManagerListener.FILE_EDITOR_MANAGER);

    notifyPublisher(new Runnable() {
      @Override
      public void run() {
        publisher.selectionChanged(event);
      }
    });
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@Nonnull VirtualFile file,
                                                                        boolean focusEditor,
                                                                        @Nonnull FileEditorWindow window) {
    return openFileWithProviders(file, focusEditor, false);
  }

  @Override
  public boolean isInsideChange() {
    return false;
  }

  @Nonnull
  @Override
  public ActionCallback notifyPublisher(@Nonnull Runnable runnable) {
    runnable.run();
    return ActionCallback.DONE;
  }

  @Override
  public FileEditorsSplitters getSplittersFor(Component c) {
    return null;
  }

  @Override
  public void createSplitter(int orientation, FileEditorWindow window) {
    String containerName = createNewTabbedContainerName();
    myTestEditorSplitter.setActiveTabGroup(containerName);
  }

  private String createNewTabbedContainerName() {
    counter++;
    return "SplitTabContainer" + ((Object) counter).toString();
  }

  @Override
  public void changeSplitterOrientation() {

  }

  @Override
  public boolean isInSplitter() {
    return false;
  }

  @Override
  public boolean hasOpenedFile() {
    return false;
  }

  @Override
  public VirtualFile getCurrentFile() {
    return myActiveFile;
  }

  @Override
  public FileEditorWithProvider getSelectedEditorWithProvider(@Nonnull VirtualFile file) {
    return null;
  }

  @Override
  public boolean isChanged(@Nonnull FileEditorComposite editor) {
    return false;
  }

  @Override
  public FileEditorWindow getNextWindow(@Nonnull FileEditorWindow window) {
    return null;
  }

  @Override
  public FileEditorWindow getPrevWindow(@Nonnull FileEditorWindow window) {
    return null;
  }

  @Override
  public void addTopComponent(@Nonnull final FileEditor editor, @Nonnull final JComponent component) {
  }

  @Override
  public void removeTopComponent(@Nonnull final FileEditor editor, @Nonnull final JComponent component) {
  }

  @Override
  public void addBottomComponent(@Nonnull final FileEditor editor, @Nonnull final JComponent component) {
  }

  @Override
  public void removeBottomComponent(@Nonnull final FileEditor editor, @Nonnull final JComponent component) {
  }

  @Override
  public void closeAllFiles() {
    for (VirtualFile file : new LinkedList<VirtualFile>(myVirtualFile2Editor.keySet())) {
      closeFile(file);
    }
  }

  private static FileEditorProvider getProvider() {
    return new FileEditorProvider() {
      @Override
      public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
        return false;
      }

      @RequiredUIAccess
      @Override
      @Nonnull
      public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
        throw new IncorrectOperationException();
      }

      @Override
      public void disposeEditor(@Nonnull FileEditor editor) {
      }

      @Override
      @Nonnull
      public FileEditorState readState(@Nonnull Element sourceElement, @Nonnull Project project, @Nonnull VirtualFile file) {
        throw new IncorrectOperationException();
      }

      @Override
      @Nonnull
      public String getEditorTypeId() {
        return "";
      }

      @Override
      @Nonnull
      public FileEditorPolicy getPolicy() {
        throw new IncorrectOperationException();
      }
    };
  }

  @Override
  public FileEditorWindow getCurrentWindow() {
    return null;
  }

  @Nonnull
  @Override
  public AsyncResult<FileEditorWindow> getActiveWindow() {
    return AsyncResult.done(null);
  }

  @Override
  public void setCurrentWindow(FileEditorWindow window) {
  }

  @Override
  public VirtualFile getFile(@Nonnull FileEditor editor) {
    return LIGHT_VIRTUAL_FILE;
  }

  @Override
  public void updateFilePresentation(@Nonnull VirtualFile file) {
  }

  @Override
  public void unsplitWindow() {

  }

  @Override
  public void unsplitAllWindow() {

  }

  @Override
  @Nonnull
  public FileEditorWindow[] getWindows() {
    return new FileEditorWindow[0];  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public FileEditor getSelectedEditor(@Nonnull VirtualFile file) {
    final Editor editor = getEditor(file);
    return editor == null ? null : TextEditorProvider.getInstance().getTextEditor(editor);
  }

  @Override
  public boolean isFileOpen(@Nonnull VirtualFile file) {
    return getEditor(file) != null;
  }

  @Override
  @Nonnull
  public FileEditor[] getEditors(@Nonnull VirtualFile file) {
    FileEditor e = getSelectedEditor(file);
    if (e == null) return new FileEditor[0];
    return new FileEditor[] {e};
  }

  @Nonnull
  @Override
  public FileEditor[] getAllEditors(@Nonnull VirtualFile file) {
    return getEditors(file);
  }

  @Override
  @Nonnull
  public VirtualFile[] getSiblings(@Nonnull VirtualFile file) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dispose() {
    closeAllFiles();
  }

  @Override
  public void closeFile(@Nonnull final VirtualFile file) {
    Editor editor = myVirtualFile2Editor.remove(file);
    if (editor != null){
      TextEditorProvider editorProvider = TextEditorProvider.getInstance();
      editorProvider.disposeEditor(editorProvider.getTextEditor(editor));
      EditorFactory.getInstance().releaseEditor(editor);
    }
    if (Comparing.equal(file, myActiveFile)) {
      myActiveFile = null;
    }

    modifyTabWell(new Runnable() {
      @Override
      public void run() {
        myTestEditorSplitter.closeFile(file);
      }
    });
  }

  @Override
  public void closeFile(@Nonnull VirtualFile file, @Nonnull FileEditorWindow window) {
    closeFile(file);
  }

  @Override
  @Nonnull
  public VirtualFile[] getSelectedFiles() {
    return myActiveFile == null ? VirtualFile.EMPTY_ARRAY : new VirtualFile[]{myActiveFile};
  }

  @Override
  @Nonnull
  public FileEditor[] getSelectedEditors() {
    return new FileEditor[0];
  }

  @Override
  public Editor getSelectedTextEditor() {
    return myActiveFile != null ? getEditor(myActiveFile) : null;
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return new JLabel();
  }

  @Override
  @Nonnull
  public VirtualFile[] getOpenFiles() {
    return VfsUtilCore.toVirtualFileArray(myVirtualFile2Editor.keySet());
  }

  public Editor getEditor(VirtualFile file) {
    return myVirtualFile2Editor.get(file);
  }

  @Override
  @Nonnull
  public FileEditor[] getAllEditors() {
    FileEditor[] result = new FileEditor[myVirtualFile2Editor.size()];
    int i = 0;
    for (Map.Entry<VirtualFile, Editor> entry : myVirtualFile2Editor.entrySet()) {
      TextEditor textEditor = TextEditorProvider.getInstance().getTextEditor(entry.getValue());
      result[i++] = textEditor;
    }
    return result;
  }

  @Override
  public void showEditorAnnotation(@Nonnull FileEditor editor, @Nonnull JComponent annotationComponent) {
  }


  @Override
  public void removeEditorAnnotation(@Nonnull FileEditor editor, @Nonnull JComponent annotationComponent) {
  }

  @Override
  public Editor openTextEditor(@Nonnull OpenFileDescriptor descriptor, boolean focusEditor) {
    final VirtualFile file = descriptor.getFile();
    Editor editor = myVirtualFile2Editor.get(file);

    if (editor == null) {
      PsiFile psiFile = PsiManager.getInstance(myProject).findFile(file);
      LOG.assertTrue(psiFile != null, file);
      Document document = PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
      LOG.assertTrue(document != null, psiFile);
      editor = EditorFactory.getInstance().createEditor(document, myProject);
      final EditorHighlighter highlighter = HighlighterFactory.createHighlighter(myProject, file);
      ((EditorEx) editor).setHighlighter(highlighter);
      ((EditorEx) editor).setFile(file);

      myVirtualFile2Editor.put(file, editor);
    }

    if (descriptor.getOffset() >= 0){
      editor.getCaretModel().moveToOffset(descriptor.getOffset());
    }
    else if (descriptor.getLine() >= 0 && descriptor.getColumn() >= 0){
      editor.getCaretModel().moveToLogicalPosition(new LogicalPosition(descriptor.getLine(), descriptor.getColumn()));
    }
    editor.getSelectionModel().removeSelection();
    myActiveFile = file;

    return editor;
  }

  @Override
  public void addFileEditorManagerListener(@Nonnull FileEditorManagerListener listener) {
  }

  @Override
  public void addFileEditorManagerListener(@Nonnull FileEditorManagerListener listener, @Nonnull Disposable parentDisposable) {
  }

  @Override
  public void removeFileEditorManagerListener(@Nonnull FileEditorManagerListener listener) {
  }

  @Override
  @Nonnull
  public List<FileEditor> openEditor(@Nonnull OpenFileDescriptor descriptor, boolean focusEditor) {
    return Collections.emptyList();
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    throw new UnsupportedOperationException();
  }

  @Override
  @Nonnull
  public Pair<FileEditor[], FileEditorProvider[]> getEditorsWithProviders(@Nonnull VirtualFile file) {

    Pair<FileEditor, FileEditorProvider> editorAndProvider = myTestEditorSplitter.getEditorAndProvider(file);

    FileEditor[] fileEditor = new FileEditor[0];
    FileEditorProvider[] fileEditorProvider= new FileEditorProvider[0];
    if (editorAndProvider != null) {
      fileEditor = new FileEditor[] {editorAndProvider.first};
      fileEditorProvider = new FileEditorProvider[]{editorAndProvider.second};
    }

    return Pair.create(fileEditor, fileEditorProvider);
  }

  @Override
  public int getWindowSplitCount() {
    return 0;
  }

  @Override
  public boolean hasSplitOrUndockedWindows() {
    return false;
  }

  @Nonnull
  @Override
  public FileEditorsSplitters getSplitters() {
    throw new IncorrectOperationException();
  }

  @Nonnull
  @Override
  public AsyncResult<Void> getReady(@Nonnull Object requestor) {
    return AsyncResult.resolved();
  }

  @Override
  public void setSelectedEditor(@Nonnull VirtualFile file, @Nonnull String fileEditorProviderId) {
  }
}
