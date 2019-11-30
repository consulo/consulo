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
package consulo.util.lang.reflect;

import consulo.logging.Logger;
import consulo.util.lang.ref.SimpleReference;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Based on IDEA version
 */
public class ReflectionUtil {
  private static class MySecurityManager extends SecurityManager {
    private static final MySecurityManager INSTANCE = new MySecurityManager();

    public Class[] getStack() {
      return getClassContext();
    }
  }

  private static final Logger LOG = Logger.getInstance(ReflectionUtil.class);

  public static boolean isAssignable(@Nonnull Class<?> ancestor, @Nonnull Class<?> descendant) {
    return ancestor == descendant || ancestor.isAssignableFrom(descendant);
  }

  public static <T> T getStaticFieldValue(@Nonnull Class objectClass, @Nullable Class<T> fieldType, @Nonnull @NonNls String fieldName) {
    try {
      final Field field = findAssignableField(objectClass, fieldType, fieldName);
      if (!Modifier.isStatic(field.getModifiers())) {
        throw new IllegalArgumentException("Field " + objectClass + "." + fieldName + " is not static");
      }
      return (T)field.get(null);
    }
    catch (NoSuchFieldException e) {
      LOG.debug(e);
      return null;
    }
    catch (IllegalAccessException e) {
      LOG.debug(e);
      return null;
    }
  }

  @Nonnull
  public static Field findAssignableField(@Nonnull Class<?> clazz, @Nullable final Class<?> fieldType, @Nonnull final String fieldName) throws NoSuchFieldException {
    Field result = processFields(clazz, field -> fieldName.equals(field.getName()) && (fieldType == null || fieldType.isAssignableFrom(field.getType())));
    if (result != null) return result;
    throw new NoSuchFieldException("Class: " + clazz + " fieldName: " + fieldName + " fieldType: " + fieldType);
  }

  private static Field processFields(@Nonnull Class clazz, @Nonnull Predicate<Field> checker) {
    SimpleReference<Field> fieldRef = SimpleReference.create();

    processClasses(clazz, it -> {
      for (Field field : it.getDeclaredFields()) {
        if (checker.test(field)) {
          fieldRef.set(field);
          return false;
        }
      }

      return true;
    });

    return fieldRef.get();
  }

  protected static void processClasses(Class<?> clazz, Predicate<Class> classConsumer) {
    processClasses(clazz, classConsumer, new HashSet<>());
  }

  protected static void processClasses(Class<?> clazz, Predicate<Class> classConsumer, Set<Class> processed) {
    if (processed.add(clazz)) {
      if (!classConsumer.test(clazz)) {
        return;
      }
    }

    for (Class<?> intf : clazz.getInterfaces()) {
      processClasses(intf, classConsumer, processed);
    }

    Class<?> superclass = clazz.getSuperclass();
    if (superclass != null) {
      processClasses(superclass, classConsumer, processed);
    }
  }

  /**
   * Returns the class this method was called 'framesToSkip' frames up the caller hierarchy.
   * <p/>
   * NOTE:
   * <b>Extremely expensive!
   * Please consider not using it.
   * These aren't the droids you're looking for!</b>
   */
  @Nullable
  public static Class findCallerClass(int framesToSkip) {
    try {
      Class[] stack = MySecurityManager.INSTANCE.getStack();
      int indexFromTop = 1 + framesToSkip;
      return stack.length > indexFromTop ? stack[indexFromTop] : null;
    }
    catch (Exception e) {
      LOG.warn(e);
      return null;
    }
  }
}
