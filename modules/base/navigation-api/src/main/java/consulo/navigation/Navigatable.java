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
package consulo.navigation;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.ui.UIAccess;
import consulo.util.dataholder.Key;

import java.util.concurrent.CompletableFuture;

public interface Navigatable {
    Navigatable[] EMPTY_ARRAY = new Navigatable[0];

    Key<Navigatable> KEY = Key.create(Navigatable.class);

    Key<Navigatable[]> KEY_OF_ARRAY = Key.create(Navigatable[].class);

    @RequiredReadAction
    @SuppressWarnings("deprecation")
    default NavigateOptions getNavigateOptions() {
        return new SimpleNavigateOptions(canNavigate(), canNavigateToSource());
    }

    /**
     * Open editor and select/navigate to the object there if possible.
     * Just do nothing if navigation is not possible like in case of a package
     *
     * @param requestFocus <code>true</code> if focus requesting is necessary
     */
    @SuppressWarnings("deprecation")
    default CompletableFuture<?> navigateAsync(UIAccess uiAccess, boolean requestFocus) {
        return uiAccess.giveAsync(() -> navigate(requestFocus));
    }

    // region deprecated methods

    /**
     * Open editor and select/navigate to the object there if possible.
     * Just do nothing if navigation is not possible like in case of a package
     *
     * @param requestFocus <code>true</code> if focus requesting is necessary
     */
    @Deprecated
    @DeprecationInfo("Use #nativeAsync()")
    default void navigate(boolean requestFocus) {
        throw new AbstractMethodError("Not implemented #nativeAsync() instead this method");
    }

    /**
     * @return <code>false</code> if navigation is not possible for any reason.
     * @deprecated Use {@link #getNavigateOptions()}
     */
    @Deprecated
    @DeprecationInfo("Use #getNavigateOptions()")
    @RequiredReadAction
    default boolean canNavigate() {
        return true;
    }

    /**
     * @return <code>false</code> if navigation to source is not possible for any reason.
     * Source means some kind of editor
     * @deprecated Use {@link #getNavigateOptions()}
     */
    @Deprecated
    @DeprecationInfo("Use #getNavigateOptions()")
    @RequiredReadAction
    default boolean canNavigateToSource() {
        return canNavigate();
    }

    // endregion
}
