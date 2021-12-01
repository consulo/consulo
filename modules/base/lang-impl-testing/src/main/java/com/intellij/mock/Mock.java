package com.intellij.mock;

import com.intellij.codeHighlighting.BackgroundEditorHighlighter;
import com.intellij.ide.structureView.StructureViewBuilder;
import consulo.disposer.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.impl.IdeFocusManagerHeadless;
import com.intellij.util.ArrayUtil;
import consulo.fileEditor.impl.EditorComposite;
import consulo.fileEditor.impl.EditorWindow;
import consulo.fileEditor.impl.EditorsSplitters;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.dataholder.UserDataHolderBase;
import kava.beans.PropertyChangeListener;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;

//[kirillk] - this class looks to be an overkill but IdeDocumentHistory is highly coupled
// with all of that stuff below, so it's not possible to test it's back/forward capabilities
// w/o making mocks for all of them. perhaps later we will decouple those things
public class Mock {

  public static class MyFileEditor extends UserDataHolderBase implements DocumentsEditor {
    public Document[] DOCUMENTS;

    @Override
    public Document[] getDocuments() {
      return DOCUMENTS;
    }

    @Override
    @Nonnull
    public JComponent getComponent() {
      throw new UnsupportedOperationException();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
      return null;
    }

    @Override
    @Nonnull
    public String getName() {
      return "";
    }

    @Override
    public void dispose() {
    }

    @Override
    public StructureViewBuilder getStructureViewBuilder() {
      return null;
    }

    @Nullable
    @Override
    public VirtualFile getFile() {
      return null;
    }

    @Override
    @Nonnull
    public FileEditorState getState(@Nonnull FileEditorStateLevel level) {
      return new FileEditorState() {
        @Override
        public boolean canBeMergedWith(FileEditorState fileEditorState, FileEditorStateLevel fileEditorStateLevel) {
          return false;
        }
      };
    }

    @Override
    public void setState(@Nonnull FileEditorState state) {
    }

    @Override
    public boolean isModified() {
      return false;
    }

    @Override
    public boolean isValid() {
      return false;
    }

    @Override
    public void selectNotify() {
    }

    @Override
    public void deselectNotify() {
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    }

    @Override
    public BackgroundEditorHighlighter getBackgroundHighlighter() {
      return null;
    }

    @Override
    public FileEditorLocation getCurrentLocation() {
      return null;
    }
  }

  public static class MyFileEditorManager extends FileEditorManagerEx {
    @Nonnull
    @Override
    public JComponent getComponent() {
      return null;
    }

    @Nonnull
    @Override
    public ActionCallback notifyPublisher(@Nonnull Runnable runnable) {
      runnable.run();
      return new ActionCallback.Done();
    }

    @Override
    public AsyncResult<Void> getReady(@Nonnull Object requestor) {
      return AsyncResult.resolved();
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@Nonnull VirtualFile file, boolean focusEditor, @Nonnull EditorWindow window) {
      throw new RuntimeException("not implemented");
    }

    @Override
    public boolean isInsideChange() {
      return false;
    }

    @Override
    public boolean hasSplitOrUndockedWindows() {
      return false;
    }

    @Override
    public EditorsSplitters getSplittersFor(Component c) {
      return null;
    }

    @Nonnull
    @Override
    public EditorsSplitters getSplitters() {
      throw new RuntimeException("not implemented");
    }

    @Nonnull
    @Override
    public AsyncResult<EditorWindow> getActiveWindow() {
      throw new RuntimeException("not implemented");
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
    public JComponent getPreferredFocusedComponent() {
      return null;
    }

    @Override
    @Nonnull
    public Pair<FileEditor[], FileEditorProvider[]> getEditorsWithProviders(@Nonnull VirtualFile file) {
      throw new UnsupportedOperationException();
    }

    public FileEditorProvider getProvider(FileEditor editor) {
      return null;
    }

    @Override
    public EditorWindow getCurrentWindow() {
      return null;
    }

    @Override
    public void setCurrentWindow(EditorWindow window) {
    }

    @Override
    public VirtualFile getFile(@Nonnull FileEditor editor) {
      return null;
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
    public EditorWindow[] getWindows() {
      return new EditorWindow[0];
    }

    @Override
    @Nonnull
    public VirtualFile[] getSiblings(@Nonnull VirtualFile file) {
      return new VirtualFile[0];
    }

    @Override
    public void createSplitter(int orientation, @Nullable EditorWindow window) {
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
      return null;
    }

    @Override
    public com.intellij.openapi.fileEditor.ex.FileEditorWithProvider getSelectedEditorWithProvider(@Nonnull VirtualFile file) {
      return null;
    }

    @Override
    public boolean isChanged(@Nonnull EditorComposite editor) {
      return false;
    }

    @Override
    public EditorWindow getNextWindow(@Nonnull EditorWindow window) {
      return null;
    }

    @Override
    public EditorWindow getPrevWindow(@Nonnull EditorWindow window) {
      return null;
    }

    @Override
    public void closeAllFiles() {
    }

    public Editor openTextEditorEnsureNoFocus(@Nonnull OpenFileDescriptor descriptor) {
      return null;
    }

    @Override
    @Nonnull
    public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(@Nonnull VirtualFile file, boolean focusEditor, boolean searchForSplitter) {
      return Pair.create(new FileEditor[0], new FileEditorProvider[0]);
    }

    @Override
    public void closeFile(@Nonnull VirtualFile file) {
    }

    @Override
    public void closeFile(@Nonnull VirtualFile file, @Nonnull EditorWindow window) {
    }

    @Override
    public Editor openTextEditor(@Nonnull OpenFileDescriptor descriptor, boolean focusEditor) {
      return null;
    }

    @Override
    public Editor getSelectedTextEditor() {
      return null;
    }

    @Override
    public boolean isFileOpen(@Nonnull VirtualFile file) {
      return false;
    }

    @Override
    @Nonnull
    public VirtualFile[] getOpenFiles() {
      return new VirtualFile[0];
    }

    @Override
    @Nonnull
    public VirtualFile[] getSelectedFiles() {
      return new VirtualFile[0];
    }

    @Override
    @Nonnull
    public FileEditor[] getSelectedEditors() {
      return new FileEditor[0];
    }

    @Override
    public FileEditor getSelectedEditor(@Nonnull VirtualFile file) {
      return null;
    }

    @Override
    @Nonnull
    public FileEditor[] getEditors(@Nonnull VirtualFile file) {
      return new FileEditor[0];
    }

    @Nonnull
    @Override
    public FileEditor[] getAllEditors(@Nonnull VirtualFile file) {
      return new FileEditor[0];
    }

    @Override
    @Nonnull
    public FileEditor[] getAllEditors() {
      return new FileEditor[0];
    }

    @Override
    public void removeEditorAnnotation(@Nonnull FileEditor editor, @Nonnull JComponent annotationComoponent) {
    }

    @Override
    public void showEditorAnnotation(@Nonnull FileEditor editor, @Nonnull JComponent annotationComoponent) {
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
      throw new UnsupportedOperationException();
    }

    @Override
    public void registerExtraEditorDataProvider(@Nonnull EditorDataProvider provider, Disposable parentDisposable) {
    }

    @Override
    public int getWindowSplitCount() {
      return 0;
    }

    @Override
    public void setSelectedEditor(@Nonnull VirtualFile file, String fileEditorProviderId) {
    }
  }

  public static class MyVirtualFile extends VirtualFile {

    public boolean myValid = true;

    @Override
    @Nonnull
    public VirtualFileSystem getFileSystem() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String getPath() {
      return null;
    }

    @Override
    @Nonnull
    public String getName() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void rename(Object requestor, @Nonnull String newName) throws IOException {
    }

    @Override
    public boolean isWritable() {
      return false;
    }

    @Override
    public boolean isDirectory() {
      return false;
    }

    @Override
    public boolean isValid() {
      return myValid;
    }

    @Override
    public VirtualFile getParent() {
      return null;
    }

    @Override
    public VirtualFile[] getChildren() {
      return new VirtualFile[0];
    }

    @Override
    public VirtualFile createChildDirectory(Object requestor, String name) throws IOException {
      return null;
    }

    @Override
    public VirtualFile createChildData(Object requestor, @Nonnull String name) throws IOException {
      return null;
    }

    @Override
    public void delete(Object requestor) throws IOException {
    }

    @Override
    public void move(Object requestor, @Nonnull VirtualFile newParent) throws IOException {
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return null;
    }

    @Override
    @Nonnull
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
      throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public byte[] contentsToByteArray() throws IOException {
      return ArrayUtil.EMPTY_BYTE_ARRAY;
    }

    @Override
    public long getModificationStamp() {
      return 0;
    }

    @Override
    public long getTimeStamp() {
      return 0;
    }

    @Override
    public long getLength() {
      return 0;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
    }
  }

  public static class MyToolWindowManager extends ToolWindowManager {

    @Override
    public boolean canShowNotification(@Nonnull String toolWindowId) {
      return false;
    }

    @Nonnull
    @Override
    public ToolWindow registerToolWindow(@Nonnull String id, @Nonnull JComponent component, @Nonnull ToolWindowAnchor anchor) {
      return null;
    }

    @Nonnull
    @RequiredUIAccess
    @Override
    public ToolWindow registerToolWindow(@Nonnull String id,
                                         @Nonnull JComponent component,
                                         @Nonnull ToolWindowAnchor anchor,
                                         Disposable parentDisposable,
                                         boolean canWorkInDumbMode,
                                         boolean canCloseContents) {
      return null;
    }

    @Nonnull
    @RequiredUIAccess
    @Override
    public ToolWindow registerToolWindow(@Nonnull final String id, final boolean canCloseContent, @Nonnull final ToolWindowAnchor anchor) {
      return null;
    }

    @Nonnull
    @RequiredUIAccess
    @Override
    public ToolWindow registerToolWindow(@Nonnull final String id, final boolean canCloseContent, @Nonnull final ToolWindowAnchor anchor, final Disposable parentDisposable, final boolean dumbAware) {
      return null;
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public ToolWindow registerToolWindow(@Nonnull String id, boolean canCloseContent, @Nonnull ToolWindowAnchor anchor, Disposable parentDisposable, boolean canWorkInDumbMode, boolean secondary) {
      return null;
    }

    @Nonnull
    @RequiredUIAccess
    @Override
    public ToolWindow registerToolWindow(@Nonnull final String id, final boolean canCloseContent, @Nonnull final ToolWindowAnchor anchor, final boolean secondary) {
      return null;
    }

    public JComponent getFocusTargetFor(final JComponent comp) {
      return null;
    }

    @RequiredUIAccess
    @Override
    public void unregisterToolWindow(@Nonnull String id) {
    }

    @Override
    public void activateEditorComponent() {
    }

    public ActionCallback requestFocus(final Component c, final boolean forced) {
      return new ActionCallback.Done();
    }

    public ActionCallback requestFocus(final ActiveRunnable command, final boolean forced) {
      return new ActionCallback.Done();
    }

    @Override
    public boolean isEditorComponentActive() {
      return false;
    }

    @Nonnull
    @Override
    public String[] getToolWindowIds() {
      return ArrayUtil.EMPTY_STRING_ARRAY;
    }

    @RequiredUIAccess
    @Override
    public String getActiveToolWindowId() {
      return null;
    }

    @Override
    public ToolWindow getToolWindow(String id) {
      return null;
    }

    @Override
    public void invokeLater(Runnable runnable) {
    }

    @Nonnull
    @Override
    public IdeFocusManager getFocusManager() {
      return IdeFocusManagerHeadless.INSTANCE;
    }

    @Override
    public void notifyByBalloon(@Nonnull final String toolWindowId,
                                @Nonnull final MessageType type,
                                @Nonnull final String text,
                                @Nullable final Image icon,
                                @Nullable final HyperlinkListener listener) {
    }

    @Override
    public Balloon getToolWindowBalloon(String id) {
      return null;
    }

    @Override
    public boolean isMaximized(@Nonnull ToolWindow wnd) {
      return false;
    }

    @Override
    public void setMaximized(@Nonnull ToolWindow wnd, boolean maximized) {

    }

    @Override
    public void notifyByBalloon(@Nonnull final String toolWindowId, @Nonnull final MessageType type, @Nonnull final String htmlBody) {
    }
  }


  public static class MyFileEditorProvider implements FileEditorProvider {
    @Override
    public boolean accept(@Nonnull Project project, @Nonnull VirtualFile file) {
      return false;
    }

    @RequiredUIAccess
    @Override
    @Nonnull
    public FileEditor createEditor(@Nonnull Project project, @Nonnull VirtualFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void disposeEditor(@Nonnull FileEditor editor) {
    }

    @Override
    @Nonnull
    public FileEditorState readState(@Nonnull Element sourceElement, @Nonnull Project project, @Nonnull VirtualFile file) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void writeState(@Nonnull FileEditorState state, @Nonnull Project project, @Nonnull Element targetElement) {
    }

    @Override
    @Nonnull
    public String getEditorTypeId() {
      throw new UnsupportedOperationException();
    }

    @Override
    @Nonnull
    public FileEditorPolicy getPolicy() {
      throw new UnsupportedOperationException();
    }
  }
}
