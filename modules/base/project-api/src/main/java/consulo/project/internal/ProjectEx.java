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
package consulo.project.internal;

import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.internal.UIAccessInternal;

public interface ProjectEx extends Project {
    int REGULAR_PROJECT = 1 << 30;
    int DEFAULT_PROJECT = 1 << 31;

    void setProjectName(String name);

    /**
     * Called again whenever the ui is replaced under a still open project - a browser refresh builds a new one
     * and closes the old. The coroutine context holds a copy of its own, so both have to be written.
     */
    default void setUIAccess(UIAccess uiAccess) {
        UIAccess original = UIAccessInternal.original(uiAccess);

        UIAccess previous = getUserData(UIAccess.KEY);
        if (previous instanceof UIAccessInternal previousInternal && previousInternal.getOriginal() != original) {
            previousInternal.releaseProtection();
        }

        UIAccess protectedAccess = original instanceof UIAccessInternal internal
            ? internal.makeProtection(getDisposed())
            : original;

        putUserData(UIAccess.KEY, protectedAccess);

        coroutineContext().putCopyableUserData(UIAccess.KEY, protectedAccess);
    }
}
