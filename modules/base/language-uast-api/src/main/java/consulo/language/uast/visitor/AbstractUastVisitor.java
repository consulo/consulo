// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.uast.visitor;

import consulo.language.uast.UElement;

/**
 * A {@link UastVisitor} that visits each element's children by default.
 */
public abstract class AbstractUastVisitor implements UastVisitor {
    @Override
    public boolean visitElement(UElement node) {
        return false;
    }
}
