/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/*
 * @author max
 */
package consulo.ide.impl.psi.impl.source.codeStyle;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.ide.impl.idea.codeStyle.CodeStyleFacade;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.impl.internal.SemanticEditorPositionFactoryImpl;
import consulo.language.codeStyle.lineIndent.LineIndentProvider;
import consulo.language.codeStyle.lineIndent.SemanticEditorPositionFactory;
import consulo.language.psi.PsiDocumentManager;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@Deprecated
public abstract class CodeStyleFacadeImpl implements CodeStyleFacade {
  private final Project myProject;

  public CodeStyleFacadeImpl() {
    this(null);
  }

  public CodeStyleFacadeImpl(Project project) {
    myProject = project;
  }

  @Override
  @Deprecated
  public int getIndentSize(FileType fileType) {
    return CodeStyle.getProjectOrDefaultSettings(myProject).getIndentSize(fileType);
  }

  @Override
  @Nullable
  @Deprecated
  public String getLineIndent(@Nonnull Document document, int offset) {
    if (myProject == null) return null;
    PsiDocumentManager.getInstance(myProject).commitDocument(document);
    return CodeStyleManager.getInstance(myProject).getLineIndent(document, offset);
  }

  @Override
  public String getLineIndent(@Nonnull Editor editor, @Nullable Language language, int offset, boolean allowDocCommit) {
    if (myProject == null) return null;
    LineIndentProvider lineIndentProvider = LineIndentProvider.findLineIndentProvider(language);
    String indent;

    if (lineIndentProvider != null) {
      SemanticEditorPositionFactory factory = new SemanticEditorPositionFactoryImpl(editor);
      Document document = editor.getDocument();
      indent = lineIndentProvider.getLineIndent(myProject, document, factory, language, offset);
    }
    else {
      indent = null;
    }

    if (indent == LineIndentProvider.DO_NOT_ADJUST) {
      return allowDocCommit ? null : indent;
    }
    //noinspection deprecation
    return indent != null ? indent : (allowDocCommit ? getLineIndent(editor.getDocument(), offset) : null);
  }

  @Override
  public String getLineSeparator() {
    return CodeStyle.getProjectOrDefaultSettings(myProject).getLineSeparator();
  }

  @Override
  public int getRightMargin(Language language) {
    return CodeStyle.getProjectOrDefaultSettings(myProject).getRightMargin(language);
  }

  @Override
  public int getTabSize(FileType fileType) {
    return CodeStyle.getProjectOrDefaultSettings(myProject).getTabSize(fileType);
  }

  @Override
  public boolean useTabCharacter(FileType fileType) {
    return CodeStyle.getProjectOrDefaultSettings(myProject).useTabCharacter(fileType);
  }
}