/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.navigation;

import consulo.annotation.access.RequiredReadAction;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * Implementation of {@link Navigable} interface which actually doesn't allow navigation. Its {@link #INSTANCE} can be passed to methods which
 * expect non-null instance of {@link Navigable} if you cannot provide a real implementation.
 */
@SuppressWarnings("deprecation")
public abstract sealed class NonNavigable implements Navigatable permits NonNavigatable {
    public static final Navigable INSTANCE = new NonNavigatable();

    @Override
    @RequiredUIAccess
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
}

