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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * General-purpose {@link ClassSet} backed by a list of classes and a per-class-name cache of the subtype check.
 */
public class ClassSetImpl<T> implements ClassSet<T> {
    private final List<Class<? extends T>> myInitialClasses;
    private final Map<String, Boolean> myInternalMapping = new ConcurrentHashMap<>();

    public ClassSetImpl(List<Class<? extends T>> initialClasses) {
        myInitialClasses = initialClasses;
        for (Class<? extends T> initialClass : initialClasses) {
            myInternalMapping.put(initialClass.getName(), Boolean.TRUE);
        }
    }

    public List<Class<? extends T>> getInitialClasses() {
        return myInitialClasses;
    }

    @Override
    public boolean isEmpty() {
        return myInitialClasses.isEmpty();
    }

    @Override
    public boolean contains(Class<? extends T> element) {
        Boolean cached = myInternalMapping.get(element.getName());
        if (cached != null) {
            return cached;
        }
        boolean result = false;
        for (Class<? extends T> initialClass : myInitialClasses) {
            if (initialClass.isAssignableFrom(element)) {
                result = true;
                break;
            }
        }
        myInternalMapping.put(element.getName(), result);
        return result;
    }

    @Override
    public String toString() {
        return "ClassSetImpl(" + myInitialClasses + ")";
    }

    @Override
    public List<Class<? extends T>> toList() {
        return myInitialClasses;
    }
}
