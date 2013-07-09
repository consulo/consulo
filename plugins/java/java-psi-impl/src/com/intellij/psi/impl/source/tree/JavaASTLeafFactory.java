/*
 * Copyright 2013 Consulo.org
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
package com.intellij.psi.impl.source.tree;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 21:13/09.07.13
 */
public class JavaASTLeafFactory extends CoreJavaASTLeafFactory {
  @Override
  @NotNull
  public LeafElement createLeaf(final IElementType type, final CharSequence text) {
    if (type == C_STYLE_COMMENT || type == END_OF_LINE_COMMENT) {
      return new PsiCommentImpl(type, text);
    }

    return super.createLeaf(type, text);
  }
}
