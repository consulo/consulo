// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.uast.internal;

import consulo.language.uast.util.ClassSet;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ClassSet} that is the union of several other class sets.
 */
public class ClassSetsWrapper<T> implements ClassSet<T> {
    private final ClassSet<T>[] mySets;

    public ClassSetsWrapper(ClassSet<T>[] sets) {
        mySets = sets;
    }

    public ClassSet<T>[] getSets() {
        return mySets;
    }

    @Override
    public boolean isEmpty() {
        for (ClassSet<T> set : mySets) {
            if (!set.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(Class<? extends T> element) {
        for (ClassSet<T> set : mySets) {
            if (set.contains(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Class<? extends T>> toList() {
        List<Class<? extends T>> result = new ArrayList<>();
        for (ClassSet<T> set : mySets) {
            result.addAll(set.toList());
        }
        return result;
    }
}
