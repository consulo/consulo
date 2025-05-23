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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.codeEditor.*;
import consulo.codeEditor.internal.stickyLine.StickyLinesModel;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.util.EditorUtil;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataContextWrapper;
import consulo.document.Document;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 18-Mar-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeEditorInternalHelper {
    public static class CaretDataContext extends DataContextWrapper {
        protected final Caret myCaret;

        public CaretDataContext(@Nonnull DataContext delegate, @Nonnull Caret caret) {
            super(delegate);
            myCaret = caret;
        }

        @Nullable
        @Override
        @SuppressWarnings("unchecked")
        public <T> T getData(@Nonnull Key<T> dataId) {
            if (Caret.KEY == dataId) {
                return (T) myCaret;
            }
            return super.getData(dataId);
        }
    }

    public static CodeEditorInternalHelper getInstance() {
        return Application.get().getInstance(CodeEditorInternalHelper.class);
    }

    @Nonnull
    default CaretDataContext createCaretDataContext(@Nonnull DataContext delegate, @Nonnull Caret caret) {
        return new CaretDataContext(delegate, caret);
    }

    default boolean ensureInjectionUpToDate(@Nonnull Caret hostCaret) {
        return false;
    }

    @Nullable
    default String getProperIndent(Project project, Document document, int offset) {
        return null;
    }

    default boolean isLexemeBoundary(@Nullable Object leftTokenType, @Nullable Object rightTokenType) {
        return !Objects.equals(leftTokenType, rightTokenType);
    }

    default void rememberEditorHighlighterForCachesOptimization(Document document, @Nonnull final EditorHighlighter highlighter) {
    }

    default void updateNotifications(Project project, VirtualFile file) {
    }

    default boolean shouldUseSmartTabs(Project project, @Nonnull Editor editor) {
        return false;
    }

    default int calcColumnNumber(@Nonnull Editor editor, @Nonnull CharSequence text, int start, int offset) {
        return calcColumnNumber(editor, text, start, offset, EditorUtil.getTabSize(editor));
    }

    default int calcColumnNumber(@Nullable Editor editor, @Nonnull CharSequence text, final int start, final int offset, final int tabSize) {
        return 0;
    }

    default int getLastVisualLineColumnNumber(@Nonnull Editor editor, final int line) {
        return line;
    }

    default int getSpaceWidth(@Nonnull Editor editor) {
        return 1;
    }

    @Nullable
    MarkupModelEx forDocument(@Nonnull Document document, @Nullable Project project, boolean create);

    default boolean isShowMethodSeparators() {
        return false;
    }

    default void setShowMethodSeparators(boolean value) {
    }

    @RequiredUIAccess
    default void showParametersHitOptions() {
    }

    default boolean hasAnyInlayExtensions() {
        return false;
    }

    @Nonnull
    default LineWrapPositionStrategy getLineWrapPositionStrategy(@Nonnull Editor editor) {
        return new DefaultLineWrapPositionStrategy();
    }

    @Nonnull
    default EditorHighlighter createEmptyHighlighter(Project project, Document document) {
        throw new UnsupportedOperationException();
    }

    default void updateFoldRegions(@Nonnull Project project, @Nonnull Editor editor) {
    }

    @Nullable
    default StickyLinesModel getStickyLinesModel(@Nonnull Project project, @Nonnull Document document) {
        return null;
    }

    @Nonnull
    default StickyLinesModel getStickyLinesModel(@Nonnull MarkupModel markupModel) {
        throw new UnsupportedOperationException();
    }

    default void restartStickyPass(@Nonnull Project project, @Nonnull Document document) {
    }
}
