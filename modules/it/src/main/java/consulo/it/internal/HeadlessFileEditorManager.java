/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorComposite;
import consulo.fileEditor.FileEditorProvider;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProvider;
import consulo.fileEditor.FileEditorsSplitters;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.navigation.OpenFileDescriptor;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ComponentContainer;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;
import java.util.Set;

/**
 * Headless {@code FileEditorManager}: the production impls live in {@code desktop-awt-ide-impl}/
 * {@code ide-impl}. No editors ever open; every query answers "nothing is open". Needed because
 * project-level services ({@code DaemonListeners}, tool-window logic, editor history) resolve it at
 * project startup; extends {@link FileEditorManagerEx} because {@code IdeDocumentHistoryImpl} casts to
 * it. Bound only under the {@link ComponentProfiles#INTEGRATION_TEST} profile.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessFileEditorManager extends FileEditorManagerEx {
    private final Project myProject;

    @Inject
    public HeadlessFileEditorManager(Project project) {
        myProject = project;
    }

    @Override
    public AsyncResult<Void> getReady(Object requestor) {
        return AsyncResult.resolved(null);
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public void closeFile(VirtualFile file) {
    }

    @Override
    public @Nullable Editor openTextEditor(OpenFileDescriptor descriptor, boolean focusEditor) {
        return null;
    }

    @Override
    public @Nullable Editor getSelectedTextEditor(boolean requiredUIThread) {
        return null;
    }

    @Override
    public boolean isFileOpen(VirtualFile file) {
        return false;
    }

    @Override
    public VirtualFile[] getOpenFiles() {
        return new VirtualFile[0];
    }

    @Override
    public VirtualFile[] getSelectedFiles() {
        return new VirtualFile[0];
    }

    @Override
    public FileEditor[] getSelectedEditors() {
        return new FileEditor[0];
    }

    @Override
    public @Nullable FileEditor getSelectedEditor(VirtualFile file) {
        return null;
    }

    @Override
    public FileEditor[] getEditors(VirtualFile file) {
        return new FileEditor[0];
    }

    @Override
    public FileEditor[] getAllEditors(VirtualFile file) {
        return new FileEditor[0];
    }

    @Override
    public FileEditor[] getAllEditors() {
        return new FileEditor[0];
    }

    @Override
    public @Nullable Disposable addTopComponent(FileEditor editor, ComponentContainer component) {
        return null;
    }

    @Override
    public void addTopComponent(FileEditor editor, JComponent component) {
    }

    @Override
    public void removeTopComponent(FileEditor editor, JComponent component) {
    }

    @Override
    public void addBottomComponent(FileEditor editor, JComponent component) {
    }

    @Override
    public void removeBottomComponent(FileEditor editor, JComponent component) {
    }

    @Override
    public void addFileEditorManagerListener(FileEditorManagerListener listener, Disposable parentDisposable) {
    }

    @Override
    public List<FileEditor> openEditor(OpenFileDescriptor descriptor, boolean focusEditor) {
        return List.of();
    }

    @Override
    public void setSelectedEditor(VirtualFile file, String fileEditorProviderId) {
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    public Pair<FileEditor[], FileEditorProvider[]> getEditorsWithProviders(VirtualFile file) {
        return Pair.create(new FileEditor[0], new FileEditorProvider[0]);
    }

    @Override
    public @Nullable VirtualFile getFile(FileEditor editor) {
        return null;
    }

    @Override
    public void updateFilePresentation(VirtualFile file) {
    }

    @Override
    public @Nullable FileEditorWindow getCurrentWindow() {
        return null;
    }

    @Override
    public AsyncResult<FileEditorWindow> getActiveWindow() {
        return AsyncResult.rejected();
    }

    @Override
    public void setCurrentWindow(FileEditorWindow window) {
    }

    @Override
    public void closeFile(VirtualFile file, FileEditorWindow window) {
    }

    @Override
    public void unsplitWindow() {
    }

    @Override
    public void unsplitAllWindow() {
    }

    @Override
    public int getWindowSplitCount() {
        return 0;
    }

    @Override
    public boolean hasSplitOrUndockedWindows() {
        return false;
    }

    @Override
    public FileEditorWindow[] getWindows() {
        return new FileEditorWindow[0];
    }

    @Override
    public VirtualFile[] getSiblings(VirtualFile file) {
        return new VirtualFile[0];
    }

    @Override
    public void createSplitter(int orientation, @Nullable FileEditorWindow window) {
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
    public @Nullable VirtualFile getCurrentFile() {
        return null;
    }

    @Override
    public @Nullable FileEditorWithProvider getSelectedEditorWithProvider(VirtualFile file) {
        return null;
    }

    @Override
    public void closeAllFiles() {
    }

    @Override
    public @Nullable FileEditorsSplitters getSplitters() {
        return null;
    }

    @Override
    public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(VirtualFile file, boolean focusEditor, boolean searchForSplitter) {
        return Pair.create(new FileEditor[0], new FileEditorProvider[0]);
    }

    @Override
    @RequiredUIAccess
    public Pair<FileEditor[], FileEditorProvider[]> openFileWithProviders(VirtualFile file, boolean focusEditor, FileEditorWindow window) {
        return Pair.create(new FileEditor[0], new FileEditorProvider[0]);
    }

    @Override
    public boolean isChanged(FileEditorComposite editor) {
        return false;
    }

    @Override
    public @Nullable FileEditorWindow getNextWindow(FileEditorWindow window) {
        return null;
    }

    @Override
    public @Nullable FileEditorWindow getPrevWindow(FileEditorWindow window) {
        return null;
    }

    @Override
    public boolean isInsideChange() {
        return false;
    }

    @Override
    public Set<FileEditorsSplitters> getAllSplitters() {
        return Set.of();
    }

    @Override
    public @Nullable FileEditorsSplitters getSplittersFor(java.awt.Component c) {
        return null;
    }

    @Override
    public ActionCallback notifyPublisher(Runnable runnable) {
        runnable.run();
        return ActionCallback.DONE;
    }
}
