// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.fileEditor;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorHighlighter;
import consulo.fileEditor.internal.largeFileEditor.*;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

public interface LargeFileEditor extends FileEditor {
    Key<Object> LARGE_FILE_EDITOR_MARK_KEY = new Key<>("lfe.editorMark");
    Key<LargeFileEditor> LARGE_FILE_EDITOR_KEY = new Key<>("lfe.editor");
    Key<Boolean> LARGE_FILE_EDITOR_SOFT_WRAP_KEY = new Key<>("lfe.soft.wrap");

    LfeSearchManager getSearchManager();

    void showSearchResult(SearchResult searchResult);

    Project getProject();

    long getCaretPageNumber();

    int getCaretPageOffset();

    Editor getEditor();

    @Override
    @Nonnull
    VirtualFile getFile();

    LargeFileEditorAccess createAccessForEncodingWidget();

    FileDataProviderForSearch getFileDataProviderForSearch();

    @Nonnull
    LargeFileEditorModel getEditorModel();

    int getPageSize();

    void trySetHighlighter(@Nonnull EditorHighlighter editorHighlighter);
}
