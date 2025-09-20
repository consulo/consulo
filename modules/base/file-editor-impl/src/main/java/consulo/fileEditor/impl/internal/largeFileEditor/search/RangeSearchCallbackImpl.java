// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor.search;


import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.LargeFileEditor;
import consulo.fileEditor.impl.internal.largeFileEditor.LargeFileEditorProvider;
import consulo.fileEditor.internal.largeFileEditor.FileDataProviderForSearch;
import consulo.fileEditor.internal.largeFileEditor.SearchResult;
import consulo.fileEditor.localize.FileEditorLocalize;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;

public final class RangeSearchCallbackImpl implements RangeSearchCallback {

    private static final Logger LOG = Logger.getInstance(RangeSearchCallbackImpl.class);

    @Override
    public FileDataProviderForSearch getFileDataProviderForSearch(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
        LargeFileEditor largeFileEditor = getLargeFileEditor(createIfNotExists, project, virtualFile);
        return largeFileEditor == null ? null : largeFileEditor.getFileDataProviderForSearch();
    }

    @Override
    public void showResultInEditor(SearchResult searchResult, Project project, VirtualFile virtualFile) {
        LargeFileEditor largeFileEditor = getLargeFileEditor(true, project, virtualFile);
        if (largeFileEditor == null) {
            Messages.showWarningDialog(FileEditorLocalize.largeFileEditorMessageCantShowFileInTheEditor().get(), FileEditorLocalize.largeFileEditorTitleShowMatchProblem().get());
            LOG.info("[Large File Editor Subsystem] Can't get LargeFileEditor for showing search result. FilePath="
                + virtualFile.getPath());
            return;
        }
        largeFileEditor.showSearchResult(searchResult);

        // select necessary tab if any other is selected
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        fileEditorManager.openFile(virtualFile, false, true);
        fileEditorManager.setSelectedEditor(virtualFile, LargeFileEditorProvider.PROVIDER_ID);
    }

    private static LargeFileEditor getLargeFileEditor(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        if (createIfNotExists) {
            FileEditor[] fileEditors = fileEditorManager.openFile(virtualFile, false, true);
            for (FileEditor fileEditor : fileEditors) {
                if (fileEditor instanceof LargeFileEditor) {
                    return (LargeFileEditor) fileEditor;
                }
            }
        }
        else {
            FileEditor[] existedFileEditors = fileEditorManager.getEditors(virtualFile);
            for (FileEditor fileEditor : existedFileEditors) {
                if (fileEditor instanceof LargeFileEditor) {
                    return (LargeFileEditor) fileEditor;
                }
            }
        }
        return null;
    }
}
