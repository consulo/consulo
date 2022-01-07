// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.ref;

import com.intellij.concurrency.ConcurrentCollectionFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PairProcessor;
import com.intellij.util.containers.FList;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderEx;
import consulo.util.lang.reflect.unsafe.UnsafeDelegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;

public final class DebugReflectionUtil {
  private static final Map<Class<?>, Field[]> allFields = Collections.synchronizedMap(ConcurrentCollectionFactory.createMap(new HashingStrategy<Class<?>>() {
    // default strategy seems to be too slow
    @Override
    public int hashCode(@Nullable Class<?> aClass) {
      return aClass == null ? 0 : aClass.getName().hashCode();
    }

    @Override
    public boolean equals(@Nullable Class<?> o1, @Nullable Class<?> o2) {
      return o1 == o2;
    }
  }));

  private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

  @Nonnull
  private static Field[] getAllFields(@Nonnull Class<?> aClass) {
    Field[] cached = allFields.get(aClass);
    if (cached == null) {
      try {
        Field[] declaredFields = aClass.getDeclaredFields();
        List<Field> fields = new ArrayList<>(declaredFields.length + 5);
        for (Field declaredField : declaredFields) {
          declaredField.setAccessible(true);
          Class<?> type = declaredField.getType();
          if (isTrivial(type)) continue; // unable to hold references, skip
          fields.add(declaredField);
        }
        Class<?> superclass = aClass.getSuperclass();
        if (superclass != null) {
          for (Field sup : getAllFields(superclass)) {
            if (!fields.contains(sup)) {
              fields.add(sup);
            }
          }
        }
        cached = fields.isEmpty() ? EMPTY_FIELD_ARRAY : fields.toArray(new Field[0]);
      }
      catch (IncompatibleClassChangeError | NoClassDefFoundError | SecurityException | InaccessibleObjectException e) {
        // this exception may be thrown because there are two different versions of org.objectweb.asm.tree.ClassNode from different plugins
        //I don't see any sane way to fix it until we load all the plugins by the same classloader in tests
        cached = EMPTY_FIELD_ARRAY;
      }

      allFields.put(aClass, cached);
    }
    return cached;
  }

  private static boolean isTrivial(@Nonnull Class<?> type) {
    return type.isPrimitive() || type == String.class || type == Class.class || type.isArray() && isTrivial(type.getComponentType());
  }

  private static boolean isInitialized(@Nonnull Class<?> root) {
    return !UnsafeDelegate.get().shouldBeInitialized(root);
  }

  private static final Key<Boolean> REPORTED_LEAKED = Key.create("REPORTED_LEAKED");

  public static boolean walkObjects(int maxDepth,
                                    @Nonnull Map<Object, String> startRoots,
                                    @Nonnull Class<?> lookFor,
                                    @Nonnull Predicate<Object> shouldExamineValue,
                                    @Nonnull PairProcessor<Object, ? super BackLink> leakProcessor) {
    IntSet visited = IntSets.newHashSet(100);
    Deque<BackLink> toVisit = new ArrayDeque<>(100);

    for (Map.Entry<Object, String> entry : startRoots.entrySet()) {
      Object startRoot = entry.getKey();
      String description = entry.getValue();
      toVisit.addLast(new BackLink(startRoot, null, null) {
        @Override
        void print(@Nonnull StringBuilder result) {
          super.print(result);
          result.append(" (from ").append(description).append(")");
        }
      });
    }

    while (true) {
      BackLink backLink;
      backLink = toVisit.pollFirst();
      if (backLink == null) {
        return true;
      }

      if (backLink.depth > maxDepth) {
        continue;
      }
      Object value = backLink.value;
      if (lookFor.isAssignableFrom(value.getClass()) && markLeaked(value) && !leakProcessor.process(value, backLink)) {
        return false;
      }

      if (visited.add(System.identityHashCode(value))) {
        queueStronglyReferencedValues(toVisit, value, shouldExamineValue, backLink);
      }
    }
  }

  private static void queueStronglyReferencedValues(@Nonnull Deque<? super BackLink> queue, @Nonnull Object root, @Nonnull Predicate<Object> shouldExamineValue, @Nonnull BackLink backLink) {
    Class<?> rootClass = root.getClass();
    for (Field field : getAllFields(rootClass)) {
      String fieldName = field.getName();
      // do not follow weak/soft refs
      if (root instanceof Reference && ("referent".equals(fieldName) || "discovered".equals(fieldName))) {
        continue;
      }

      Object value;
      try {
        value = field.get(root);
      }
      catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }

      queue(value, field, backLink, queue, shouldExamineValue);
    }
    if (rootClass.isArray()) {
      try {
        for (Object value : (Object[])root) {
          queue(value, null, backLink, queue, shouldExamineValue);
        }
      }
      catch (ClassCastException ignored) {
      }
    }
    // check for objects leaking via static fields. process initialized classes only
    if (root instanceof Class && isInitialized((Class<?>)root)) {
      for (Field field : getAllFields((Class<?>)root)) {
        if ((field.getModifiers() & Modifier.STATIC) == 0) continue;
        try {
          Object value = field.get(null);
          queue(value, field, backLink, queue, shouldExamineValue);
        }
        catch (IllegalAccessException ignored) {
        }
      }
    }
  }

  private static void queue(Object value, Field field, @Nonnull BackLink backLink, @Nonnull Deque<? super BackLink> queue, @Nonnull Predicate<Object> shouldExamineValue) {
    if (value == null || isTrivial(value.getClass())) {
      return;
    }
    if (shouldExamineValue.test(value)) {
      queue.addLast(new BackLink(value, field, backLink));
    }
  }

  private static boolean markLeaked(Object leaked) {
    return !(leaked instanceof UserDataHolderEx) || ((UserDataHolderEx)leaked).replace(REPORTED_LEAKED, null, Boolean.TRUE);
  }

  public static class BackLink {
    @Nonnull
    private final Object value;
    private final Field field;
    private final BackLink backLink;
    private final int depth;

    BackLink(@Nonnull Object value, @Nullable Field field, @Nullable BackLink backLink) {
      this.value = value;
      this.field = field;
      this.backLink = backLink;
      depth = backLink == null ? 0 : backLink.depth + 1;
    }

    @Override
    public String toString() {
      StringBuilder result = new StringBuilder();
      BackLink backLink = this;
      while (backLink != null) {
        backLink.print(result);
        backLink = backLink.backLink;
      }
      return result.toString();
    }

    void print(@Nonnull StringBuilder result) {
      String valueStr;
      Object value = this.value;
      try {
        if (value instanceof FList) {
          valueStr = "FList (size=" + ((FList<?>)value).size() + ")";
        }
        else {
          valueStr = value instanceof Collection ? "Collection (size=" + ((Collection<?>)value).size() + ")" : String.valueOf(value);
        }
        valueStr = StringUtil.first(StringUtil.convertLineSeparators(valueStr, "\\n"), 200, true);
      }
      catch (Throwable e) {
        valueStr = "(" + e.getMessage() + " while computing .toString())";
      }

      Field field = this.field;
      String fieldName = field == null ? "?" : field.getDeclaringClass().getName() + "." + field.getName();
      result.append("via '").append(fieldName).append("'; Value: '").append(valueStr).append("' of ").append(value.getClass()).append("\n");
    }
  }
}
