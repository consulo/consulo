// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.impl.internal.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.endpoint.util.SimpleNamePomTarget;
import consulo.language.pom.PomTarget;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

class DelegateSimpleNamePomTarget extends SimpleNamePomTarget {
    private final PomTarget myDelegate;
    private final @Nullable Image myIcon;
    private final LocalizeValue myTypeName;

    DelegateSimpleNamePomTarget(PomTarget delegate, String name, @Nullable Image icon, LocalizeValue typeName) {
        super(name);
        myDelegate = delegate;
        myIcon = icon;
        myTypeName = typeName;
    }

    PomTarget getDelegate() {
        return myDelegate;
    }

    @Nullable Image getIcon() {
        return myIcon;
    }

    LocalizeValue getTypeName() {
        return myTypeName;
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
        myDelegate.navigate(requestFocus);
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return myDelegate.canNavigate();
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        return myDelegate.canNavigateToSource();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (!super.equals(other)) {
            return false;
        }

        DelegateSimpleNamePomTarget that = (DelegateSimpleNamePomTarget) other;

        return myDelegate.equals(that.myDelegate);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + myDelegate.hashCode();
        return result;
    }
}
