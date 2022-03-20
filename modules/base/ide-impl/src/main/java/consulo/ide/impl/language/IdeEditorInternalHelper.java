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

import com.intellij.codeStyle.CodeStyleFacade;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.internal.EditorInternalHelper;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.fileEditor.FileEditorManager;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 18-Mar-22
 */
@Singleton
public class IdeEditorInternalHelper extends EditorInternalHelper {
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
          if (data != null) return (T)data;
        }
      }
      return super.getData(dataId);
    }
  }

  @Inject
  public IdeEditorInternalHelper(Project project) {
    super(project);
  }

  @Nullable
  @Override
  public String getProperIndent(Document document, int offset) {
    PsiDocumentManager.getInstance(myProject).commitDocument(document); // Sync document and PSI before formatting.
    return offset >= document.getTextLength() ? "" : CodeStyleFacade.getInstance(myProject).getLineIndent(document, offset);
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
}
