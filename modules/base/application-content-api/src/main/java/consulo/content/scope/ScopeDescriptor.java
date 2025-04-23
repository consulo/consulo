// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.content.scope;

import consulo.application.util.ColoredItem;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nullable;

/**
 * @author anna
 */
public class ScopeDescriptor implements ColoredItem {
    private final SearchScope myScope;

    public ScopeDescriptor(@Nullable SearchScope scope) {
        myScope = scope;
    }

    public String getDisplayName() {
        return myScope == null ? null : myScope.getDisplayName();
    }

    @Nullable
    public Image getIcon() {
        return myScope == null ? null : myScope.getIcon();
    }

    @Nullable
    public SearchScope getScope() {
        return myScope;
    }

    public boolean scopeEquals(SearchScope scope) {
        return Comparing.equal(myScope, scope);
    }

    @Nullable
    @Override
    public ColorValue getColor() {
        return myScope instanceof ColoredItem ? ((ColoredItem)myScope).getColor() : null;
    }
}
