/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.language;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ReadAction;
import consulo.codeEditor.*;
import consulo.codeEditor.impl.DocumentMarkupModelImpl;
import consulo.codeEditor.impl.StickyLinesCollector;
import consulo.codeEditor.impl.StickyLinesModelImpl;
import consulo.codeEditor.impl.util.EditorImplUtil;
import consulo.codeEditor.internal.CodeEditorInternalHelper;
import consulo.codeEditor.internal.stickyLine.StickyLinesModel;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.codeStyle.CodeStyleFacade;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.editor.impl.EditorHighlighterCache;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.DaemonCodeAnalyzerSettings;
import consulo.language.editor.LanguageLineWrapPositionStrategy;
import consulo.language.editor.action.WordBoundaryFilter;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.folding.CodeFoldingManager;
import consulo.language.editor.highlight.EmptyEditorHighlighter;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import java.awt.*;

/**
 * @author VISTALL
 * @since 18-Mar-22
 */
@Singleton
@ServiceImpl
public class LanguageCodeEditorInternalHelper implements CodeEditorInternalHelper {
    private static class FileEditorAffectCaretContext extends CaretDataContext {

        public FileEditorAffectCaretContext(@Nonnull DataContext delegate, @Nonnull Caret caret) {
            super(delegate, caret);
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getData(@Nonnull Key<T> dataId) {
            Project project = super.getData(Project.KEY);
            if (project != null) {
                FileEditorManager fm = FileEditorManager.getInstance(project);
                if (fm != null) {
                    Object data = fm.getData(dataId, myCaret.getEditor(), myCaret);
                    if (data != null) {
                        return (T) data;
                    }
                }
            }
            return super.getData(dataId);
        }
    }

    private final Provider<DaemonCodeAnalyzerSettings> myDaemonCodeAnalyzerSettings;

    @Inject
    public LanguageCodeEditorInternalHelper(Provider<DaemonCodeAnalyzerSettings> daemonCodeAnalyzerSettings) {
        myDaemonCodeAnalyzerSettings = daemonCodeAnalyzerSettings;
    }

    @Nullable
    @Override
    public String getProperIndent(Project project, Document document, int offset) {
        PsiDocumentManager.getInstance(project).commitDocument(document); // Sync document and PSI before formatting.
        return offset >= document.getTextLength() ? "" : CodeStyleFacade.getInstance(project).getLineIndent(document, offset);
    }

    @Nonnull
    @Override
    public CaretDataContext createCaretDataContext(@Nonnull DataContext delegate, @Nonnull Caret caret) {
        return new FileEditorAffectCaretContext(delegate, caret);
    }

    @Override
    public boolean ensureInjectionUpToDate(@Nonnull Caret hostCaret) {
        Editor editor = hostCaret.getEditor();
        Project project = editor.getProject();
        if (project != null && InjectedLanguageManager.getInstance(project).mightHaveInjectedFragmentAtOffset(editor.getDocument(), hostCaret.getOffset())) {
            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
            return true;
        }
        return false;
    }

    @Override
    public boolean isLexemeBoundary(@Nullable Object left, @Nullable Object right) {
        IElementType leftTokenType = (IElementType) left;
        IElementType rightTokenType = (IElementType) right;
        return leftTokenType != null && rightTokenType != null && WordBoundaryFilter.forLanguage(rightTokenType.getLanguage()).isWordBoundary(leftTokenType, rightTokenType);
    }

    @Override
    public void rememberEditorHighlighterForCachesOptimization(Document document, @Nonnull EditorHighlighter highlighter) {
        if (!(highlighter instanceof EmptyEditorHighlighter)) {
            EditorHighlighterCache.rememberEditorHighlighterForCachesOptimization(document, highlighter);
        }
    }

    @Override
    public void updateNotifications(Project project, VirtualFile file) {
        EditorNotifications.getInstance(project).updateNotifications(file);
    }

    @Override
    public boolean shouldUseSmartTabs(Project project, @Nonnull Editor editor) {
        if (!(editor instanceof EditorEx)) {
            return false;
        }
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        return CodeStyleSettingsManager.getSettings(project).getIndentOptionsByFile(file).SMART_TABS;
    }

    @Override
    public int calcColumnNumber(@Nullable Editor editor, @Nonnull CharSequence text, int start, int offset, int tabSize) {
        return EditorImplUtil.calcColumnNumber(editor, text, start, offset, tabSize);
    }

    @Override
    public int getLastVisualLineColumnNumber(@Nonnull Editor editor, int line) {
        return EditorImplUtil.getLastVisualLineColumnNumber(editor, line);
    }

    @Override
    public int getSpaceWidth(@Nonnull Editor editor) {
        return EditorImplUtil.getSpaceWidth(Font.PLAIN, editor);
    }

    @Nonnull
    @Override
    public MarkupModelEx forDocument(@Nonnull Document document, @Nullable Project project, boolean create) {
        return DocumentMarkupModelImpl.forDocument(document, project, create);
    }

    @Override
    public void setShowMethodSeparators(boolean value) {
        myDaemonCodeAnalyzerSettings.get().SHOW_METHOD_SEPARATORS = value;
    }

    @Override
    public boolean isShowMethodSeparators() {
        return myDaemonCodeAnalyzerSettings.get().SHOW_METHOD_SEPARATORS;
    }

    @Nonnull
    @Override
    public LineWrapPositionStrategy getLineWrapPositionStrategy(@Nonnull Editor editor) {
        return LanguageLineWrapPositionStrategy.forEditor(editor);
    }

    @Nonnull
    @Override
    public EditorHighlighter createEmptyHighlighter(Project project, Document document) {
        return EditorUtil.createEmptyHighlighter(project, document);
    }

    @Override
    public void updateFoldRegions(@Nonnull Project project, @Nonnull Editor editor) {
        CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(project);
        foldingManager.updateFoldRegions(editor);
    }

    @Nullable
    @Override
    public StickyLinesModel getStickyLinesModel(@Nonnull Project project, @Nonnull Document document) {
        return StickyLinesModelImpl.getModel(project, document);
    }

    @Nonnull
    @Override
    public StickyLinesModel getStickyLinesModel(@Nonnull MarkupModel markupModel) {
        return StickyLinesModelImpl.getModel(markupModel);
    }

    @Override
    public void restartStickyPass(@Nonnull Project project, @Nonnull Document document) {
        ReadAction.nonBlocking(() -> {
                StickyLinesCollector collector = new StickyLinesCollector(project, document);
                collector.forceCollectPass();

                DaemonCodeAnalyzer.getInstance(project).restart();
            })
            .expireWith(project)
            .submitDefault();
    }

    @Override
    public int compareByHighlightInfoSeverity(@Nonnull RangeHighlighterEx o1, @Nonnull RangeHighlighterEx o2) {
        HighlightInfo info1 = HighlightInfo.fromRangeHighlighter(o1);
        HighlightInfo info2 = HighlightInfo.fromRangeHighlighter(o2);
        HighlightSeverity severity1 = info1 == null ? null : info1.getSeverity();
        HighlightSeverity severity2 = info2 == null ? null : info2.getSeverity();
        if (severity1 != null && severity2 != null) {
            // higher severity should win
            return severity2.compareTo(severity1);
        }
        // having severity has more priority than no severity
        return Boolean.compare(severity1 == null, severity2 == null);
    }
}
