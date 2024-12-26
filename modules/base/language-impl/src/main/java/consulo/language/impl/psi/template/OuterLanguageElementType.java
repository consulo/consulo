// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.psi.template;

import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.ILeafElementType;
import consulo.language.ast.IReparseableLeafElementType;
import consulo.language.template.ITemplateDataElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Element type that may be used for representing outer fragments in templating language.
 *
 * @see ITemplateDataElementType
 * @see OuterLanguageElementImpl
 * @see IReparseableLeafElementType
 */
public class OuterLanguageElementType extends IElementType implements ILeafElementType {
    public OuterLanguageElementType(@Nonnull String debugName,
                                    @Nonnull Language language) {
        super(debugName, language);
    }

    protected OuterLanguageElementType(@Nonnull String debugName,
                                       @Nullable Language language, boolean register) {
        super(debugName, language, register);
    }

    @Override
    @Nonnull
    public ASTNode createLeafNode(@Nonnull CharSequence leafText) {
        return new OuterLanguageElementImpl(this, leafText);
    }
}
