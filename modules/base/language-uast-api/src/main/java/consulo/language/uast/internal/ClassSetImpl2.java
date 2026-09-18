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

/**
 * {@link ClassSet} of exactly two classes.
 */
public class ClassSetImpl2<T> implements ClassSet<T> {
    private final Class<? extends T> myAClazz;
    private final Class<? extends T> myBClazz;
    private final List<Class<? extends T>> myList;

    public ClassSetImpl2(Class<? extends T> aClazz, Class<? extends T> bClazz) {
        myAClazz = aClazz;
        myBClazz = bClazz;
        myList = List.of(aClazz, bClazz);
    }

    public Class<? extends T> getAClazz() {
        return myAClazz;
    }

    public Class<? extends T> getBClazz() {
        return myBClazz;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(Class<? extends T> element) {
        return myAClazz.isAssignableFrom(element)
            || myBClazz.isAssignableFrom(element);
    }

    @Override
    public String toString() {
        return "ClassSetImpl2(" + myAClazz + ", " + myBClazz + ")";
    }

    @Override
    public List<Class<? extends T>> toList() {
        return myList;
    }
}
