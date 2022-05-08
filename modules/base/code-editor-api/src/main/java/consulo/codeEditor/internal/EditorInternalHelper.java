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

import consulo.application.Application;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.util.EditorUtil;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataContextWrapper;
import consulo.document.Document;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 18-Mar-22
 */
@Singleton
public interface EditorInternalHelper {
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
      if (Caret.KEY == dataId) return (T)myCaret;
      return super.getData(dataId);
    }
  }

  public static EditorInternalHelper getInstance() {
    return Application.get().getInstance(EditorInternalHelper.class);
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
}
