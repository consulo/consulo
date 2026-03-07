/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.fileEditor.impl.internal;

import consulo.application.Application;
import consulo.codeEditor.*;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.FileDocumentManager;
import consulo.document.LazyRangeMarkerFactory;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.fileEditor.*;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.navigation.Navigatable;
import consulo.navigation.NavigateOptions;
import consulo.navigation.OpenFileDescriptor;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInManager;
import consulo.project.ui.view.SelectInTarget;
import consulo.ui.UIAccess;
import consulo.ui.ex.coroutine.UIAction;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineContext;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.CompletableFutureStep;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UnprotectedUserDataHolder;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.INativeFileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class OpenFileDescriptorImpl extends UnprotectedUserDataHolder implements Navigatable, OpenFileDescriptor {
    /**
     * Tells descriptor to navigate in specific editor rather than file editor in main IDEA window.
     * For example if you want to navigate in editor embedded into modal dialog, you should provide this data.
     */
    public static final Key<Editor> NAVIGATE_IN_EDITOR = Key.create("NAVIGATE_IN_EDITOR");

    private final Project myProject;
    private final VirtualFile myFile;
    private final int myLogicalLine;
    private final int myLogicalColumn;
    private final int myOffset;
    private final RangeMarker myRangeMarker;

    private boolean myUseCurrentWindow = false;

    public OpenFileDescriptorImpl(@Nonnull Project project, @Nonnull VirtualFile file, int offset) {
        this(project, file, -1, -1, offset, false);
    }

    public OpenFileDescriptorImpl(@Nonnull Project project, @Nonnull VirtualFile file, int logicalLine, int logicalColumn) {
        this(project, file, logicalLine, logicalColumn, -1, false);
    }

    public OpenFileDescriptorImpl(@Nonnull Project project,
                                  @Nonnull VirtualFile file,
                                  int logicalLine,
                                  int logicalColumn,
                                  boolean persistent) {
        this(project, file, logicalLine, logicalColumn, -1, persistent);
    }

    public OpenFileDescriptorImpl(@Nonnull Project project, @Nonnull VirtualFile file) {
        this(project, file, -1, -1, -1, false);
    }

    public OpenFileDescriptorImpl(@Nonnull Project project,
                                  @Nonnull VirtualFile file,
                                  int logicalLine,
                                  int logicalColumn,
                                  int offset,
                                  boolean persistent) {
        myProject = project;
        myFile = file;
        myLogicalLine = logicalLine;
        myLogicalColumn = logicalColumn;
        myOffset = offset;
        if (offset >= 0) {
            myRangeMarker = LazyRangeMarkerFactory.getInstance(project).createRangeMarker(file, offset);
        }
        else if (logicalLine >= 0) {
            myRangeMarker =
                LazyRangeMarkerFactory.getInstance(project).createRangeMarker(file, logicalLine, Math.max(0, logicalColumn), persistent);
        }
        else {
            myRangeMarker = null;
        }
    }

    @Override
    @Nonnull
    public VirtualFile getFile() {
        return myFile;
    }

    @Nullable
    public RangeMarker getRangeMarker() {
        return myRangeMarker;
    }

    @Override
    public int getOffset() {
        return myRangeMarker != null && myRangeMarker.isValid() ? myRangeMarker.getStartOffset() : myOffset;
    }

    @Override
    public int getLine() {
        return myLogicalLine;
    }

    @Override
    public int getColumn() {
        return myLogicalColumn;
    }

    @Override
    public boolean isValid() {
        RangeMarker rangeMarker = getRangeMarker();
        return rangeMarker == null || rangeMarker.isValid();
    }

    @Nonnull
    @Override
    public CompletableFuture<?> navigateAsync(@Nonnull UIAccess uiAccess, boolean requestFocus) {
        if (!getNavigateOptions().canNavigate()) {
            return CompletableFuture.failedFuture(new IllegalStateException("target not valid"));
        }

        CoroutineContext coroutineContext = myProject == null
            ? Application.get().coroutineContext()
            : myProject.coroutineContext();

        return CoroutineScope.launchAsync(coroutineContext, () -> {
            return Coroutine
                // Step 1 (UIAction): Check file type, handle native/directory/requested editor
                .first(UIAction.<Void, Boolean>apply((input, continuation) -> {
                    FileType type = FileTypeRegistry.getInstance().getKnownFileTypeOrAssociate(myFile, myProject);
                    if (type == null || !myFile.isValid()) {
                        continuation.finishEarly(null);
                        return Boolean.FALSE;
                    }

                    if (type instanceof INativeFileType nativeType) {
                        nativeType.openFileInAssociatedApplication(myProject, myFile);
                        continuation.finishEarly(null);
                        return Boolean.FALSE;
                    }

                    if (myFile.isDirectory()) {
                        navigateInProjectView(requestFocus);
                        continuation.finishEarly(null);
                        return Boolean.FALSE;
                    }

                    // Try to navigate in a requested editor first
                    if (navigateInRequestedEditor()) {
                        continuation.finishEarly(null);
                        return Boolean.FALSE;
                    }

                    return Boolean.TRUE; // need to open file
                }))

                // Step 2 (CompletableFutureStep): Open file asynchronously using openFileAsync
                .then(CompletableFutureStep.<Boolean, FileEditorOpenResult>await(needsOpen -> {
                    // Resolve VirtualFileWindow to host file
                    VirtualFile fileToOpen = myFile;
                    if (myFile instanceof VirtualFileWindow virtualFileWindow) {
                        fileToOpen = virtualFileWindow.getDelegate();
                    }

                    FileEditorOpenOptions options = new FileEditorOpenOptions()
                        .withFocusEditor(requestFocus)
                        .withSearchForSplitter(!myUseCurrentWindow);

                    return FileEditorManager.getInstance(myProject)
                        .openFileAsync(fileToOpen, options);
                }))

                // Step 3 (UIAction): Navigate in opened editors
                .then(UIAction.<FileEditorOpenResult, Void>apply(result -> {
                    if (result == null || result.isEmpty()) {
                        return null;
                    }

                    // Resolve descriptor for navigation (handle VirtualFileWindow offset)
                    OpenFileDescriptorImpl descriptor = OpenFileDescriptorImpl.this;
                    if (myFile instanceof VirtualFileWindow virtualFileWindow) {
                        int hostOffset = virtualFileWindow.getDocumentWindow().injectedToHost(getOffset());
                        descriptor = new OpenFileDescriptorImpl(myProject, virtualFileWindow.getDelegate(), hostOffset);
                        descriptor.setUseCurrentWindow(myUseCurrentWindow);
                    }

                    navigateInOpenedEditors(result, descriptor, requestFocus);

                    return null;
                }));
        }).toFuture();
    }

    private void navigateInOpenedEditors(
        @Nonnull FileEditorOpenResult result,
        @Nonnull OpenFileDescriptorImpl descriptor,
        boolean requestFocus
    ) {
        FileEditor[] editors = result.getEditors();

        // Try the selected editor first
        boolean navigated = false;
        FileEditor selectedEditor = FileEditorManager.getInstance(myProject).getSelectedEditor(descriptor.getFile());

        for (FileEditor editor : editors) {
            if (editor instanceof NavigatableFileEditor navEditor && editor == selectedEditor) {
                if (navEditor.canNavigateTo(descriptor)) {
                    navEditor.navigateTo(descriptor);
                    navigated = true;
                    break;
                }
            }
        }

        // Try other editors
        if (!navigated) {
            for (FileEditor editor : editors) {
                if (editor instanceof NavigatableFileEditor navEditor && editor != selectedEditor) {
                    if (navEditor.canNavigateTo(descriptor)) {
                        navEditor.navigateTo(descriptor);
                        break;
                    }
                }
            }
        }

        // Unfold + focus for TextEditors
        for (FileEditor editor : editors) {
            if (editor instanceof TextEditor textEditor) {
                Editor e = textEditor.getEditor();
                unfoldCurrentLine(e);
                if (requestFocus && myProject.getApplication().isSwingApplication()) {
                    ProjectIdeFocusManager.getInstance(myProject)
                        .requestFocus(e.getContentComponent(), true);
                }
                break;
            }
        }
    }

    @Override
    public void navigate(boolean requestFocus) {
        if (!getNavigateOptions().canNavigate()) {
            throw new IllegalStateException("target not valid");
        }

        if (!myFile.isDirectory() && navigateInEditorOrNativeApp(myProject, requestFocus)) {
            return;
        }

        navigateInProjectView(requestFocus);
    }

    private boolean navigateInEditorOrNativeApp(@Nonnull Project project, boolean requestFocus) {
        FileType type = FileTypeRegistry.getInstance().getKnownFileTypeOrAssociate(myFile, project);
        if (type == null || !myFile.isValid()) {
            return false;
        }

        if (type instanceof INativeFileType) {
            ((INativeFileType) type).openFileInAssociatedApplication(project, myFile);
            return true;
        }

        return navigateInEditor(project, requestFocus);
    }

    public boolean navigateInEditor(@Nonnull Project project, boolean requestFocus) {
        return navigateInRequestedEditor() || navigateInAnyFileEditor(project, requestFocus);
    }

    @Override
    public boolean navigateInEditor(boolean requestFocus) {
        return navigateInEditor(myProject, requestFocus);
    }

    private boolean navigateInRequestedEditor() {
        @SuppressWarnings("deprecation") DataContext ctx = DataManager.getInstance().getDataContext();
        Editor e = ctx.getData(NAVIGATE_IN_EDITOR);
        if (e == null) {
            return false;
        }
        if (!Objects.equals(FileDocumentManager.getInstance().getFile(e.getDocument()), myFile)) {
            return false;
        }

        navigateIn(e);
        return true;
    }

    private boolean navigateInAnyFileEditor(Project project, boolean focusEditor) {
        List<FileEditor> editors = FileEditorManager.getInstance(project).openEditor(this, focusEditor);
        for (FileEditor editor : editors) {
            if (editor instanceof TextEditor) {
                Editor e = ((TextEditor) editor).getEditor();
                unfoldCurrentLine(e);
                if (focusEditor) {
                    if (project.getApplication().isSwingApplication()) {
                        ProjectIdeFocusManager.getInstance(myProject).requestFocus(e.getContentComponent(), true);
                    }
                }
            }
        }
        return !editors.isEmpty();
    }

    private void navigateInProjectView(boolean requestFocus) {
        SelectInContext context = new SelectInContext() {
            @Override
            @Nonnull
            public Project getProject() {
                return myProject;
            }

            @Override
            @Nonnull
            public VirtualFile getVirtualFile() {
                return myFile;
            }

            @Override
            @Nullable
            public Object getSelectorInFile() {
                return null;
            }
        };

        for (SelectInTarget target : SelectInManager.getInstance(myProject).getTargets()) {
            if (target.canSelect(context)) {
                target.selectIn(context, requestFocus);
                return;
            }
        }
    }

    public void navigateIn(@Nonnull Editor e) {
        int offset = getOffset();
        CaretModel caretModel = e.getCaretModel();
        boolean caretMoved = false;
        if (myLogicalLine >= 0) {
            LogicalPosition pos = new LogicalPosition(myLogicalLine, Math.max(myLogicalColumn, 0));
            if (offset < 0 || offset == e.logicalPositionToOffset(pos)) {
                caretModel.removeSecondaryCarets();
                caretModel.moveToLogicalPosition(pos);
                caretMoved = true;
            }
        }
        if (!caretMoved && offset >= 0) {
            caretModel.removeSecondaryCarets();
            caretModel.moveToOffset(Math.min(offset, e.getDocument().getTextLength()));
            caretMoved = true;
        }

        if (caretMoved) {
            e.getSelectionModel().removeSelection();
            scrollToCaret(e);
            unfoldCurrentLine(e);
        }
    }

    private static void unfoldCurrentLine(@Nonnull Editor editor) {
        final FoldRegion[] allRegions = editor.getFoldingModel().getAllFoldRegions();
        final TextRange range = getRangeToUnfoldOnNavigation(editor);
        editor.getFoldingModel().runBatchFoldingOperation(new Runnable() {
            @Override
            public void run() {
                for (FoldRegion region : allRegions) {
                    if (!region.isExpanded() && range.intersects(TextRange.create(region))) {
                        region.setExpanded(true);
                    }
                }
            }
        });
    }

    @Nonnull
    public static TextRange getRangeToUnfoldOnNavigation(@Nonnull Editor editor) {
        int offset = editor.getCaretModel().getOffset();
        int line = editor.getDocument().getLineNumber(offset);
        int start = editor.getDocument().getLineStartOffset(line);
        int end = editor.getDocument().getLineEndOffset(line);
        return new TextRange(start, end);
    }

    private void scrollToCaret(@Nonnull Editor e) {
        ScrollType scrollType = getUserData(ScrollType.KEY);
        if (scrollType == null) {
            scrollType = ScrollType.CENTER;
        }

        e.getScrollingModel().scrollToCaret(scrollType);
    }

    @Override
    public NavigateOptions getNavigateOptions() {
        return myFile.isValid() ? NavigateOptions.CAN_NAVIGATE_FULL : NavigateOptions.CANT_NAVIGATE;
    }

    @Override
    @Nonnull
    public Project getProject() {
        return myProject;
    }

    public OpenFileDescriptorImpl setUseCurrentWindow(boolean search) {
        myUseCurrentWindow = search;
        return this;
    }

    @Override
    public boolean isUseCurrentWindow() {
        return myUseCurrentWindow;
    }

    @Override
    public void dispose() {
        if (myRangeMarker != null) {
            myRangeMarker.dispose();
        }
    }

    @Override
    public int compareTo(OpenFileDescriptor o) {
        int i = myProject.getName().compareTo(((Project) o.getProject()).getName());
        if (i != 0) {
            return i;
        }
        i = myFile.getName().compareTo(o.getFile().getName());
        if (i != 0) {
            return i;
        }
        RangeMarker rangeMarker = ((OpenFileDescriptorImpl) o).getRangeMarker();
        if (myRangeMarker != null) {
            if (rangeMarker == null) {
                return 1;
            }
            i = myRangeMarker.getStartOffset() - rangeMarker.getStartOffset();
            if (i != 0) {
                return i;
            }
            return myRangeMarker.getEndOffset() - rangeMarker.getEndOffset();
        }
        return rangeMarker == null ? 0 : -1;
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getUserData(@Nonnull Key<T> key) {
        if (key == RangeMarker.KEY) {
            return (T) myRangeMarker;
        }
        return super.getUserData(key);
    }
}
