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
package consulo.codeEditor.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.codeEditor.*;
import consulo.codeEditor.internal.stickyLine.StickyLinesModel;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.markup.RangeHighlighterEx;
import consulo.codeEditor.util.EditorUtil;
import consulo.colorScheme.TextAttributesKey;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataContextWrapper;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 18-Mar-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeEditorInternalHelper {
    public static class CaretDataContext extends DataContextWrapper {
        protected final Caret myCaret;

        public CaretDataContext(DataContext delegate, Caret caret) {
            super(delegate);
            myCaret = caret;
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getData(Key<T> dataId) {
            if (Caret.KEY == dataId) {
                return (T) myCaret;
            }
            return super.getData(dataId);
        }
    }

    public static CodeEditorInternalHelper getInstance() {
        return Application.get().getInstance(CodeEditorInternalHelper.class);
    }

    
    default CaretDataContext createCaretDataContext(DataContext delegate, Caret caret) {
        return new CaretDataContext(delegate, caret);
    }

    default boolean ensureInjectionUpToDate(Caret hostCaret) {
        return false;
    }

    @Nullable
    default String getProperIndent(Project project, Document document, int offset) {
        return null;
    }

    default boolean isLexemeBoundary(@Nullable Object leftTokenType, @Nullable Object rightTokenType) {
        return !Objects.equals(leftTokenType, rightTokenType);
    }

    default void rememberEditorHighlighterForCachesOptimization(Document document, EditorHighlighter highlighter) {
    }

    default void updateNotifications(Project project, VirtualFile file) {
    }

    default boolean shouldUseSmartTabs(Project project, Editor editor) {
        return false;
    }

    default int calcColumnNumber(Editor editor, CharSequence text, int start, int offset) {
        return calcColumnNumber(editor, text, start, offset, EditorUtil.getTabSize(editor));
    }

    default int calcColumnNumber(@Nullable Editor editor, CharSequence text, int start, int offset, int tabSize) {
        return 0;
    }

    default int getLastVisualLineColumnNumber(Editor editor, int line) {
        return line;
    }

    default int getSpaceWidth(Editor editor) {
        return 1;
    }

    @Nullable
    MarkupModelEx forDocument(Document document, @Nullable Project project, boolean create);

    default boolean isShowMethodSeparators() {
        return false;
    }

    default void setShowMethodSeparators(boolean value) {
    }

    
    default LineWrapPositionStrategy getLineWrapPositionStrategy(Editor editor) {
        return new DefaultLineWrapPositionStrategy();
    }

    
    default EditorHighlighter createEmptyHighlighter(Project project, Document document) {
        throw new UnsupportedOperationException();
    }

    default void updateFoldRegions(Project project, Editor editor) {
    }

    @Nullable
    default StickyLinesModel getStickyLinesModel(Project project, Document document) {
        return null;
    }

    
    default StickyLinesModel getStickyLinesModel(MarkupModel markupModel) {
        throw new UnsupportedOperationException();
    }

    default void restartStickyPass(Project project, Document document) {
    }

    default int compareByHighlightInfoSeverity(RangeHighlighterEx o1, RangeHighlighterEx o2) {
        return 0;
    }

    default void hideCursorInEditor(Editor editor) {
    }

    default void includeCurrentCommandAsNavigation(Project project) {
    }

    @RequiredReadAction
    default void checkNotIndentLines(Project project, Document document, List<Integer> nonModifiableLines, int startIndex, int endIndex) {
    }

    @RequiredUIAccess
    default boolean requestWriting(Editor editor) {
        return true;
    }
}
