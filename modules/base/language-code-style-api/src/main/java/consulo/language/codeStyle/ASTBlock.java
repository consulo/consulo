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
package consulo.language.codeStyle;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Contract;

/**
 * @author yole
 */
public interface ASTBlock extends Block {
    ASTNode getNode();

    /**
     * @return {@link ASTNode} from this {@code block} if it's {@link ASTBlock}, null otherwise
     */
    @Contract("null -> null")
    @Nullable
    static ASTNode getNode(@Nullable Block block) {
        return block instanceof ASTBlock ? ((ASTBlock) block).getNode() : null;
    }

    /**
     * @return element type of the {@link ASTNode} contained in the {@code block}, if it's an {@link ASTBlock}, null otherwise
     */
    @Contract("null -> null")
    @Nullable
    static IElementType getElementType(@Nullable Block block) {
        ASTNode node = getNode(block);
        return node == null ? null : node.getElementType();
    }

    /**
     * @return {@link PsiElement} from {@link ASTNode} from this {@code block} if it's {@link ASTBlock}, null otherwise
     */
    @Contract("null -> null")
    @Nullable
    static PsiElement getPsiElement(@Nullable Block block) {
        ASTNode obj = getNode(block);
        return obj == null ? null : obj.getPsi();
    }
}
