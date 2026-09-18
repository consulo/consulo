// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.uast;

import org.jspecify.annotations.Nullable;

/**
 * A base interface for every {@link UElement} which have a name identifier. As analogy to {@link consulo.language.psi.PsiNameIdentifierOwner}
 */
public interface UAnchorOwner extends UElement {
    @Nullable UIdentifier getUastAnchor();
}
