/*
 * Copyright 2013-2018 consulo.io
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

package consulo.language.codeStyle;

import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenType;

import jakarta.annotation.Nullable;

/**
 * from kotlin
 */
public class FormatterTreeUtil {
  @Nullable
  public static ASTNode prev(ASTNode node) {
    ASTNode prev = node.getTreePrev();
    while (prev != null && prev.getElementType() == TokenType.WHITE_SPACE) {
      prev = prev.getTreePrev();
    }
    if (prev != null) {
      return prev;
    }

    if (node.getTreeParent() != null) {
      return prev(node.getTreeParent());
    }
    return null;
  }
}
