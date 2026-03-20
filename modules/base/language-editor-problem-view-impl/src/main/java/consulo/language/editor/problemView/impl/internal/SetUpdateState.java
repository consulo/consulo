// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.problemView.impl.internal;

import java.util.Set;

public enum SetUpdateState {
    ADDED,
    REMOVED,
    UPDATED,
    IGNORED;

    public static <T> SetUpdateState add(T element, Set<T> set) {
        return set.add(element) ? ADDED : update(element, set);
    }

    public static <T> SetUpdateState remove(T element, Set<T> set) {
        return set.remove(element) ? REMOVED : IGNORED;
    }

    public static <T> SetUpdateState update(T element, Set<T> set) {
        return !set.remove(element) ? IGNORED : set.add(element) ? UPDATED : REMOVED;
    }
}
