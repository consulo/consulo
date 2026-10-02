// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.pom.PomRenameableTarget;
import org.jspecify.annotations.Nullable;

public class SimpleNamePomTarget implements PomRenameableTarget<@Nullable Object> {
    private String myName;

    public SimpleNamePomTarget(String name) {
        myName = name;
    }

    @Override
    public @Nullable Object setName(String newName) {
        myName = newName;
        return null;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    @RequiredReadAction
    public void navigate(boolean requestFocus) {
    }

    @Override
    @RequiredReadAction
    public boolean canNavigate() {
        return false;
    }

    @Override
    @RequiredReadAction
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        SimpleNamePomTarget that = (SimpleNamePomTarget) other;
        return myName.equals(that.myName);
    }

    @Override
    public int hashCode() {
        return myName.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + myName + ")";
    }
}
