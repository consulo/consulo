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
package consulo.ide.impl.idea.codeStyle;

import consulo.codeEditor.Editor;
import consulo.codeStyle.ApplicationCodeStyleFacade;
import consulo.codeStyle.ProjectCodeStyleFacade;
import consulo.document.Document;
import consulo.ide.ServiceManager;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface CodeStyleFacade {
  public static CodeStyleFacade getInstance() {
    return ServiceManager.getService(ApplicationCodeStyleFacade.class);
  }

  public static CodeStyleFacade getInstance(@Nullable Project project) {
    if (project == null) return getInstance();
    return ServiceManager.getService(project, ProjectCodeStyleFacade.class);
  }

  /**
   * Calculates the indent that should be used for the line at specified offset in the specified
   * document.
   *
   * @param document the document for which the indent should be calculated.
   * @param offset   the caret offset in the editor.
   * @return the indent string (containing of tabs and/or white spaces), or null if it
   * was not possible to calculate the indent.
   * @deprecated Use {@link #getLineIndent(Editor, Language, int, boolean)} instead.
   */
  @SuppressWarnings("DeprecatedIsStillUsed")
  @Nullable
  @Deprecated
  public abstract String getLineIndent(@Nonnull Document document, int offset);

  /**
   * Calculates the indent that should be used for the line at specified offset in the specified
   * editor. If there is a suitable {@code LineIndentProvider} for the language, it will be used to calculate the indent. Otherwise, if
   * {@code allowDocCommit} flag is true, the method will use formatter on committed document.
   *
   * @param editor         The editor for which the indent must be returned.
   * @param language       Context language
   * @param offset         The caret offset in the editor.
   * @param allowDocCommit Allow calculation using committed document.
   *                       <p>
   *                       <b>NOTE: </b> Committing the document may be slow an cause performance issues on large files.
   * @return the indent string (containing of tabs and/or white spaces), or null if it
   * was not possible to calculate the indent.
   */
  @Nullable
  default String getLineIndent(@Nonnull Editor editor, @Nullable Language language, int offset, boolean allowDocCommit) {
    //noinspection deprecation
    return getLineIndent(editor.getDocument(), offset);
  }

  /**
   * @deprecated Use {@link CodeStyle#getIndentSize(PsiFile)} instead.
   */
  @Deprecated
  public abstract int getIndentSize(FileType fileType);

  /**
   * @deprecated Use {@code getRightMargin(Language)} method of {@code CodeStyle.getSettings(PsiFile)} or
   * {@code CodeStyle.getSettings(Project)} if there is no {@code PsiFile}
   */
  @Deprecated
  public abstract int getRightMargin(Language language);

  /**
   * @deprecated Use {@code CodeStyle.getIndentOptions(PsiFile).TAB_SIZE}. See {@code CodeStyle for more information}
   */
  @Deprecated
  public abstract int getTabSize(final FileType fileType);

  /**
   * @deprecated Use {@code CodeStyle.getIndentOptions(PsiFile).USE_TAB_CHARACTER}. See {@code CodeStyle for more information}
   */
  @Deprecated
  public abstract boolean useTabCharacter(final FileType fileType);

  /**
   * @deprecated Use {@code getLineSeparator()} method of {@code CodeStyle.getSettings(PsiFile)} or
   * {@code CodeStyle.getSettings(Project)} if there is no {@code PsiFile}
   */
  @Deprecated
  public abstract String getLineSeparator();
}