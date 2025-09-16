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
package consulo.ide.impl.idea.util;

import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;

import java.lang.reflect.Constructor;
import java.util.function.Supplier;

@Deprecated
public class NewInstanceFactory<T> implements Supplier<T> {
    private static final Logger LOG = Logger.getInstance(NewInstanceFactory.class);
    private final Constructor myConstructor;
    private final Object[] myArgs;

    private NewInstanceFactory(Constructor constructor, Object[] args) {
        myConstructor = constructor;
        myArgs = args;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get() {
        try {
            return (T) myConstructor.newInstance(myArgs);
        }
        catch (Exception e) {
            LOG.error(e);
            return null;
        }
    }

    public static <T> Supplier<T> fromClass(Class<T> clazz) {
        try {
            return new NewInstanceFactory<>(clazz.getConstructor(ArrayUtil.EMPTY_CLASS_ARRAY), ArrayUtil.EMPTY_OBJECT_ARRAY);
        }
        catch (NoSuchMethodException e) {
            return () -> {
                try {
                    return clazz.newInstance();
                }
                catch (Exception e1) {
                    LOG.error(e1);
                    return null;
                }
            };
        }
    }
}
