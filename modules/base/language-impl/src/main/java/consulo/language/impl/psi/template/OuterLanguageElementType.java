// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.psi.template;

import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.ILeafElementType;
import consulo.language.ast.IReparseableLeafElementType;
import consulo.language.template.ITemplateDataElementType;
import org.jspecify.annotations.Nullable;

/**
 * Element type that may be used for representing outer fragments in template language.
 *
 * @see ITemplateDataElementType
 * @see OuterLanguageElementImpl
 * @see IReparseableLeafElementType
 */
public class OuterLanguageElementType extends IElementType implements ILeafElementType {
    public OuterLanguageElementType(String debugName,
                                    Language language) {
        super(debugName, language);
    }

    protected OuterLanguageElementType(String debugName,
                                       @Nullable Language language, boolean register) {
        super(debugName, language, register);
    }

    @Override
    
    public ASTNode createLeafNode(CharSequence leafText) {
        return new OuterLanguageElementImpl(this, leafText);
    }
}
