// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileEditor.impl.internal.largeFileEditor;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorBundle;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.event.CaretEvent;
import consulo.codeEditor.event.CaretListener;
import consulo.disposer.Disposer;
import consulo.document.internal.DocumentEx;
import consulo.document.internal.DocumentFactory;
import consulo.document.internal.FileDocumentManagerEx;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.FileEditorState;
import consulo.fileEditor.FileEditorStateLevel;
import consulo.fileEditor.LargeFileEditor;
import consulo.fileEditor.impl.internal.largeFileEditor.search.LfeSearchManagerImpl;
import consulo.fileEditor.impl.internal.largeFileEditor.search.RangeSearchCreatorImpl;
import consulo.fileEditor.internal.largeFileEditor.*;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.undoRedo.util.UndoUtil;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import kava.beans.PropertyChangeListener;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

public final class LargeFileEditorImpl extends UserDataHolderBase implements LargeFileEditor {

    private static final Logger logger = Logger.getInstance(LargeFileEditorImpl.class);
    private final Project project;
    private LargeFileManager fileManager;
    private final LargeFileEditorModelImpl editorModel;
    private final VirtualFile vFile;
    private LfeSearchManager searchManager;

    public LargeFileEditorImpl(Project project, VirtualFile vFile) {
        this.vFile = vFile;
        this.project = project;

        int customPageSize = PropertiesGetter.getPageSize();
        int customBorderShift = PropertiesGetter.getMaxPageBorderShiftBytes();

        DocumentEx document = createSpecialDocument();

        editorModel = new LargeFileEditorModelImpl(document, project, implementDataProviderForEditorModel());
        editorModel.putUserDataToEditor(LARGE_FILE_EDITOR_MARK_KEY, new Object());
        editorModel.putUserDataToEditor(LARGE_FILE_EDITOR_KEY, this);
        editorModel.putUserDataToEditor(LARGE_FILE_EDITOR_SOFT_WRAP_KEY, true);

        try {
            fileManager = new LargeFileManagerImpl(vFile, customPageSize, customBorderShift);
        }
        catch (FileNotFoundException e) {
            logger.warn(e);
            editorModel.setBrokenMode();
            Messages.showWarningDialog(EditorBundle.message("large.file.editor.message.cant.open.file.because.file.not.found"),
                EditorBundle.message("large.file.editor.title.warning"));
            requestClosingEditorTab();
            return;
        }

        searchManager = new LfeSearchManagerImpl(this, fileManager.getFileDataProviderForSearch(), new RangeSearchCreatorImpl());

        //TODO PlatformActionsReplacer.makeAdaptingOfPlatformActionsIfNeed();

        editorModel.addCaretListener(new MyCaretListener());

        fileManager.addFileChangeListener((Page lastPage, boolean isLengthIncreased) -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                editorModel.onFileChanged(lastPage, isLengthIncreased);
            });
        });
    }

    private void requestClosingEditorTab() {
        ApplicationManager.getApplication().invokeLater(
            () -> FileEditorManager.getInstance(project).closeFile(vFile));
    }

    @Override
    public LfeSearchManager getSearchManager() {
        return searchManager;
    }

    @Override
    public @Nonnull JComponent getComponent() {
        return editorModel.getComponent();
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return editorModel.getEditor().getContentComponent();
    }

    @Override
    public @Nonnull String getName() {
        return EditorBundle.message("large.file.editor.title");
    }

    @Override
    public void setState(@Nonnull FileEditorState state) {
        if (state instanceof LargeFileEditorState largeFileEditorState) {
            editorModel.setCaretAndShow(largeFileEditorState.caretPageNumber,
                largeFileEditorState.caretSymbolOffsetInPage);
        }
    }

    @Override
    public @Nonnull FileEditorState getState(@Nonnull FileEditorStateLevel level) {
        LargeFileEditorState state = new LargeFileEditorState();
        state.caretPageNumber = editorModel.getCaretPageNumber();
        state.caretSymbolOffsetInPage = editorModel.getCaretPageOffset();
        return state;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void addPropertyChangeListener(@Nonnull kava.beans.PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener) {
    }

    @Override
    public void dispose() {
        if (searchManager != null) {
            searchManager.dispose();
        }
        if (fileManager != null) {
            Disposer.dispose(fileManager);
        }
        editorModel.dispose();

        vFile.putUserData(FileDocumentManagerEx.HARD_REF_TO_DOCUMENT_KEY, null);
    }

    @RequiredUIAccess
    @Override
    public void showSearchResult(SearchResult searchResult) {
        editorModel.showSearchResult(searchResult);
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public void trySetHighlighter(@Nonnull EditorHighlighter highlighter) {
        editorModel.trySetHighlighter(highlighter);
    }

    @Override
    public long getCaretPageNumber() {
        return editorModel.getCaretPageNumber();
    }

    @Override
    public int getCaretPageOffset() {
        return editorModel.getCaretPageOffset();
    }

    @Override
    public Editor getEditor() {
        return editorModel.getEditor();
    }

    @Override
    public @Nonnull VirtualFile getFile() {
        return vFile;
    }

    @Override
    public LargeFileEditorAccess createAccessForEncodingWidget() {
        return new LargeFileEditorAccess() {
            @Override
            public @Nonnull VirtualFile getVirtualFile() {
                return getFile();
            }

            @Override
            public @Nonnull Editor getEditor() {
                return LargeFileEditorImpl.this.getEditor();
            }

            @Override
            public boolean tryChangeEncoding(@Nonnull Charset charset) {

                if (fileManager.hasBOM()) {
                    Messages.showWarningDialog(
                        EditorBundle.message("large.file.editor.message.cant.change.encoding.because.it.has.bom.byte.order.mark"),
                        EditorBundle.message("large.file.editor.title.warning"));
                    return false;
                }

                if (searchManager.isSearchWorkingNow()) {
                    Messages.showInfoMessage(EditorBundle.message("large.file.editor.message.cant.change.encoding.because.search.is.working.now"),
                        EditorBundle.message("large.file.editor.title.cant.change.encoding"));
                    return false;
                }

                fileManager.reset(charset);
                editorModel.onEncodingChanged();
                return true;
            }

            @Override
            public String getCharsetName() {
                return fileManager.getCharsetName();
            }
        };
    }

    @Override
    public FileDataProviderForSearch getFileDataProviderForSearch() {
        return fileManager.getFileDataProviderForSearch();
    }

    @Override
    public @Nonnull LargeFileEditorModel getEditorModel() {
        return editorModel;
    }

    @Override
    public int getPageSize() {
        return fileManager.getPageSize();
    }

    private static DocumentEx createSpecialDocument() {
        DocumentFactory documentFactory = DocumentFactory.getInstance();
        DocumentEx doc = documentFactory.createDocument("", false, false); // restrict "\r\n" line separators
        doc.putUserData(FileDocumentManagerEx.NOT_RELOADABLE_DOCUMENT_KEY,
            new Object());  // to protect document from illegal content changes (see usages of the key)
        UndoUtil.disableUndoFor(doc); // disabling Undo-functionality, provided by IDEA
        return doc;
    }

    private final class MyCaretListener implements CaretListener {
        @Override
        public void caretPositionChanged(@Nonnull CaretEvent e) {
            searchManager.onCaretPositionChanged(e);
        }
    }

    private LargeFileEditorModelImpl.DataProvider implementDataProviderForEditorModel() {
        return new LargeFileEditorModelImpl.DataProvider() {
            @Override
            public Page getPage(long pageNumber) throws IOException {
                return fileManager.getPage_wait(pageNumber);
            }

            @Override
            public long getPagesAmount() throws IOException {
                return fileManager.getPagesAmount();
            }

            @Override
            public Project getProject() {
                return project;
            }

            @Override
            public void requestReadPage(long pageNumber, ReadingPageResultHandler readingPageResultHandler) {
                fileManager.requestReadPage(pageNumber, readingPageResultHandler);
            }

            @Override
            public List<SearchResult> getSearchResultsInPage(Page page) {
                if (searchManager != null) {
                    return searchManager.getSearchResultsInPage(page);
                }
                return null;
            }
        };
    }
}