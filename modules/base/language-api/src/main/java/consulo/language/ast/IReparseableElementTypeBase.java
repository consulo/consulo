// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.ast;

import consulo.language.Language;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * An additional interface to be implemented by {@link IElementType} instances for tokens, which allows for incremental reparse.
 * When the infrastructure detects that all the document's changes are inside an AST node with reparseable type,
 * {@link #isParsable(ASTNode, CharSequence, Language, Project)} is invoked, and if it's successful,
 * only the contents inside this element are reparsed instead of the whole file. This can speed up reparse dramatically.
 * <p>
 * Implementers of this interface (except {@link IReparseableElementType}) must also implement {@link ICustomParsingType}.
 */
public interface IReparseableElementTypeBase extends ILazyParseableElementTypeBase {

  /**
   * Checks if the specified character sequence can be parsed as a valid content of the
   * chameleon node.
   *
   * @param parent       parent node of old (or collapsed) reparseable node.
   *                     Use this parameter only if you really understand what are doing.
   *                     Known valid use-case:
   *                     Indent-based languages. You should know about parent indent in order to decide if block is reparseable with
   *                     given text. Because if indent of some line became equals to parent indent then the block should have another
   *                     parent or block is not block anymore. So it cannot be reparsed and whole file or parent block should be reparsed.
   * @param buffer       the content to parse.
   * @param fileLanguage language of the file
   * @param project      the project containing the content.
   * @return true if the content is valid, false if not
   */
  default boolean isParsable(@Nullable ASTNode parent, @Nonnull CharSequence buffer, @Nonnull Language fileLanguage, @Nonnull Project project) {
    return false;
  }

  default boolean isValidReparse(@Nonnull ASTNode oldNode, @Nonnull ASTNode newNode) {
    return true;
  }
}
