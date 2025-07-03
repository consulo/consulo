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
package consulo.language.impl.ast;

import consulo.language.impl.ast.TokenWrapper;
import consulo.language.impl.psi.ForeignLeafPsiElement;
import consulo.language.ast.IElementType;
import consulo.language.ast.ILeafElementType;
import consulo.language.ast.ASTNode;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class ForeignLeafType extends TokenWrapper implements ILeafElementType {
    public ForeignLeafType(IElementType delegate, CharSequence value) {
        super(delegate, value);
    }

    @Override
    @Nonnull
    public ASTNode createLeafNode(CharSequence leafText) {
        return new ForeignLeafPsiElement(this, getValue());
    }
}
