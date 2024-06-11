/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.editor.impl.internal.template;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenType;
import consulo.language.codeStyle.internal.CoreCodeStyleUtil;
import consulo.language.impl.ast.FileElement;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiParserFacade;
import consulo.language.util.CharTable;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11-Jun-24
 */
public class TemplateCodeStyleHelper {
  private static final String DUMMY_IDENTIFIER = "xxx";

  /**
   * Allows to check if given offset points to white space element within the given PSI file and return that white space
   * element in the case of positive answer.
   *
   * @param file   target file
   * @param offset offset that might point to white space element within the given PSI file
   * @return target white space element for the given offset within the given file (if any); {@code null} otherwise
   */
  @Nullable
  public static PsiElement findWhiteSpaceNode(@Nonnull PsiFile file, int offset) {
    return doFindWhiteSpaceNode(file, offset).first;
  }

  @Nonnull
  private static Pair<PsiElement, CharTable> doFindWhiteSpaceNode(@Nonnull PsiFile file, int offset) {
    ASTNode astNode = SourceTreeToPsiMap.psiElementToTree(file);
    if (!(astNode instanceof FileElement)) {
      return new Pair<>(null, null);
    }
    PsiElement elementAt = InjectedLanguageManager.getInstance(file.getProject()).findInjectedElementAt(file, offset);
    final CharTable charTable = ((FileElement)astNode).getCharTable();
    if (elementAt == null) {
      elementAt = CoreCodeStyleUtil.findElementInTreeWithFormatterEnabled(file, offset);
    }

    if (elementAt == null) {
      return new Pair<>(null, charTable);
    }
    ASTNode node = elementAt.getNode();
    if (node == null || node.getElementType() != TokenType.WHITE_SPACE) {
      return new Pair<>(null, charTable);
    }
    return Pair.create(elementAt, charTable);
  }

  /**
   * Formatter trims line that contains white spaces symbols only, however, there is a possible case that we want
   * to preserve them for particular line
   * (e.g. for live template that defines line with whitespaces that contains $END$ marker: templateText   $END$).
   * <p/>
   * Current approach is to do the following:
   * <pre>
   * <ol>
   *   <li>Insert dummy text at the end of the blank line which white space symbols should be preserved;</li>
   *   <li>Perform formatting;</li>
   *   <li>Remove dummy text;</li>
   * </ol>
   * </pre>
   * <p/>
   * This method inserts that dummy comment (fallback to identifier {@code xxx}, see {@link CodeStyleManagerImpl#createDummy(PsiFile)})
   * if necessary.
   * <p/>
   *
   * <b>Note:</b> it's expected that the whole white space region that contains given offset is processed in a way that all
   * {@link RangeMarker range markers} registered for the given offset are expanded to the whole white space region.
   * E.g. there is a possible case that particular range marker serves for defining formatting range, hence, its start/end offsets
   * are updated correspondingly after current method call and whole white space region is reformatted.
   *
   * @param file     target PSI file
   * @param document target document
   * @param offset   offset that defines end boundary of the target line text fragment (start boundary is the first line's symbol)
   * @return text range that points to the newly inserted dummy text if any; {@code null} otherwise
   * @throws IncorrectOperationException if given file is read-only
   */
  @Nullable
  @RequiredReadAction
  public static TextRange insertNewLineIndentMarker(@Nonnull PsiFile file, @Nonnull Document document, int offset) {
    CharSequence text = document.getImmutableCharSequence();
    if (offset <= 0 || offset >= text.length() || !isWhiteSpaceSymbol(text.charAt(offset))) {
      return null;
    }

    if (!isWhiteSpaceSymbol(text.charAt(offset - 1))) {
      return null; // no whitespaces before offset
    }

    int end = offset;
    for (; end < text.length(); end++) {
      if (text.charAt(end) == '\n') {
        break; // line is empty till the end
      }
      if (!isWhiteSpaceSymbol(text.charAt(end))) {
        return null;
      }
    }

    CoreCodeStyleUtil.setSequentialProcessingAllowed(false);
    String dummy = createDummy(file);
    document.insertString(offset, dummy);
    return new TextRange(offset, offset + dummy.length());
  }

  private static boolean isWhiteSpaceSymbol(char c) {
    return c == ' ' || c == '\t' || c == '\n';
  }

  @Nonnull
  @RequiredReadAction
  private static String createDummy(@Nonnull PsiFile file) {
    Language language = file.getLanguage();
    PsiComment comment = null;
    try {
      comment = PsiParserFacade.getInstance(file.getProject()).createLineOrBlockCommentFromText(language, "");
    }
    catch (Throwable ignored) {
    }
    String text = comment != null ? comment.getText() : null;
    return text != null ? text : DUMMY_IDENTIFIER;
  }
}
