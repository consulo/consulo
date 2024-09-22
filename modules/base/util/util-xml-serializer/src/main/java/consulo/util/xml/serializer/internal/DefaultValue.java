/*
 * Copyright 2013-2024 consulo.io
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
package consulo.util.xml.serializer.internal;

import jakarta.annotation.Nonnull;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-09-22
 */
public class DefaultValue {
    public static final Map<Class, Object> ourDefaultValues = new HashMap<>();

    static {
        ourDefaultValues.put(float.class, (float) 0);
        ourDefaultValues.put(double.class, (double) 0);

        ourDefaultValues.put(byte.class, (byte) 0);
        ourDefaultValues.put(short.class, (short) 0);
        ourDefaultValues.put(int.class, (int) 0);
        ourDefaultValues.put(long.class, (long) 0);
        
        ourDefaultValues.put(char.class, (char) 0);

        ourDefaultValues.put(boolean.class, false);
    }

    public static <T> T createDefaultInstance(@Nonnull Class<T> clazz) {
        if (clazz.isRecord()) {
            try {
                Constructor<T> declaredConstructor = clazz.getDeclaredConstructor();
                declaredConstructor.setAccessible(true);
                return declaredConstructor.newInstance();
            }
            catch (NoSuchMethodException e) {
                // there no empty constructor
                RecordComponent[] components = clazz.getRecordComponents();
                List<Class<?>> paramTypes = new ArrayList<>(components.length);
                List<Object> arguments = new ArrayList<>(components.length);
                for (RecordComponent component : components) {
                    Object defaultValue = ourDefaultValues.get(component.getType());

                    paramTypes.add(component.getType());
                    arguments.add(defaultValue);
                }

                try {
                    return clazz.getDeclaredConstructor(paramTypes.toArray(Class[]::new)).newInstance(arguments.toArray());
                }
                catch (Throwable t) {
                    throw new RuntimeException(t);
                }
            }
            catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            return InternalReflectionUtil.newInstance(clazz);
        }
    }
}
