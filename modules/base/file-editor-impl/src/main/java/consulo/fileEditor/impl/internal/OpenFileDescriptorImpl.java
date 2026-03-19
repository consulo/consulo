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

import consulo.codeEditor.*;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.FileDocumentManager;
import consulo.document.LazyRangeMarkerFactory;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptor;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInManager;
import consulo.project.ui.view.SelectInTarget;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UnprotectedUserDataHolder;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.fileType.INativeFileType;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

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

    public OpenFileDescriptorImpl(Project project, VirtualFile file, int offset) {
        this(project, file, -1, -1, offset, false);
    }

    public OpenFileDescriptorImpl(Project project, VirtualFile file, int logicalLine, int logicalColumn) {
        this(project, file, logicalLine, logicalColumn, -1, false);
    }

    public OpenFileDescriptorImpl(Project project,
                                  VirtualFile file,
                                  int logicalLine,
                                  int logicalColumn,
                                  boolean persistent) {
        this(project, file, logicalLine, logicalColumn, -1, persistent);
    }

    public OpenFileDescriptorImpl(Project project, VirtualFile file) {
        this(project, file, -1, -1, -1, false);
    }

    public OpenFileDescriptorImpl(Project project,
                                  VirtualFile file,
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
    
    public VirtualFile getFile() {
        return myFile;
    }

    public @Nullable RangeMarker getRangeMarker() {
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

    @Override
    public void navigate(boolean requestFocus) {
        if (!canNavigate()) {
            throw new IllegalStateException("target not valid");
        }

        if (!myFile.isDirectory() && navigateInEditorOrNativeApp(myProject, requestFocus)) {
            return;
        }

        navigateInProjectView(requestFocus);
    }

    private boolean navigateInEditorOrNativeApp(Project project, boolean requestFocus) {
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

    public boolean navigateInEditor(Project project, boolean requestFocus) {
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
            
            public Project getProject() {
                return myProject;
            }

            @Override
            
            public VirtualFile getVirtualFile() {
                return myFile;
            }

            @Override
            public @Nullable Object getSelectorInFile() {
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

    public void navigateIn(Editor e) {
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

    private static void unfoldCurrentLine(Editor editor) {
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

    
    public static TextRange getRangeToUnfoldOnNavigation(Editor editor) {
        int offset = editor.getCaretModel().getOffset();
        int line = editor.getDocument().getLineNumber(offset);
        int start = editor.getDocument().getLineStartOffset(line);
        int end = editor.getDocument().getLineEndOffset(line);
        return new TextRange(start, end);
    }

    private void scrollToCaret(Editor e) {
        ScrollType scrollType = getUserData(ScrollType.KEY);
        if (scrollType == null) {
            scrollType = ScrollType.CENTER;
        }

        e.getScrollingModel().scrollToCaret(scrollType);
    }

    @Override
    public boolean canNavigate() {
        return myFile.isValid();
    }

    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }

    @Override
    
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
    public <T> T getUserData(Key<T> key) {
        if (key == RangeMarker.KEY) {
            return (T) myRangeMarker;
        }
        return super.getUserData(key);
    }
}
