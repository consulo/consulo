/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.testFramework;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Stack;
import com.intellij.util.io.PersistentEnumerator;
import com.intellij.util.ui.UIUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.*;

/**
 * User: cdr
 */
public class LeakHunter {
  private static final Map<Class, Field[]> allFields = new HashMap<Class, Field[]>();
  private static final Field[] EMPTY_FIELD_ARRAY = new Field[0];

  private static Field[] getAllFields(@Nonnull Class aClass) {
    Field[] cached = allFields.get(aClass);
    if (cached == null) {
      Field[] declaredFields = aClass.getDeclaredFields();
      List<Field> fields = new ArrayList<Field>(declaredFields.length + 5);
      for (Field declaredField : declaredFields) {
        declaredField.setAccessible(true);
        fields.add(declaredField);
      }
      Class superclass = aClass.getSuperclass();
      if (superclass != null) {
        for (Field sup : getAllFields(superclass)) {
          if (!fields.contains(sup)) {
            fields.add(sup);
          }
        }
      }
      cached = fields.isEmpty() ? EMPTY_FIELD_ARRAY : fields.toArray(new Field[fields.size()]);
      allFields.put(aClass, cached);
    }
    return cached;
  }

  private static final Set<Object> visited = ContainerUtil.<Object>newIdentityTroveSet();
  private static class BackLink {
    private final Class aClass;
    private final Object value;
    private final Field field;
    private final BackLink backLink;

    private BackLink(@Nonnull Class aClass, @Nonnull Object value, Field field, BackLink backLink) {
      this.aClass = aClass;
      this.value = value;
      this.field = field;
      this.backLink = backLink;
    }
  }

  private static final Stack<BackLink> toVisit = new Stack<BackLink>();
  private static void walkObjects(@Nonnull Class lookFor, @Nonnull Processor<BackLink> leakProcessor) {
    while (true) {
      if (toVisit.isEmpty()) return;
      BackLink backLink = toVisit.pop();
      Object root = backLink.value;
      if (!visited.add(root)) continue;
      Class rootClass = backLink.aClass;
      for (Field field : getAllFields(rootClass)) {
        String fieldName = field.getName();
        if (root instanceof Reference && "referent".equals(fieldName)) continue; // do not follow weak/soft refs
        Object value;
        try {
          value = field.get(root);
        }
        catch (IllegalArgumentException e) {
          throw new RuntimeException(e);
        }
        catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
        if (value == null) continue;
        if (value instanceof PsiElement || value instanceof TreeElement)  {
          int i = 0;
        }
        Class valueClass = value.getClass();
        if (lookFor.isAssignableFrom(valueClass) && isReallyLeak(field, fieldName, value, valueClass)) {
          BackLink newBackLink = new BackLink(valueClass, value, field, backLink);
          leakProcessor.process(newBackLink);
        }
        else {
          BackLink newBackLink = new BackLink(valueClass, value, field, backLink);
          if (toFollow(valueClass)) {
            toVisit.push(newBackLink);
          }
        }
      }
      if (rootClass.isArray()) {
        if (toFollow(rootClass.getComponentType())) {
          try {
            for (Object o : (Object[])root) {
              if (o == null) continue;
              Class oClass = o.getClass();
              toVisit.push(new BackLink(oClass, o, null, backLink));
            }
          }
          catch (ClassCastException ignored) {
          }
        }
      }
    }
  }

  private static final Key<Boolean> IS_NOT_A_LEAK = Key.create("IS_NOT_A_LEAK");
  public static void markAsNotALeak(@Nonnull UserDataHolder object) {
    object.putUserData(IS_NOT_A_LEAK, Boolean.TRUE);
  }
  private static boolean isReallyLeak(Field field, String fieldName, Object value, Class valueClass) {
    return !(value instanceof UserDataHolder) || ((UserDataHolder)value).getUserData(IS_NOT_A_LEAK) == null;
  }

  private static final Set<String> noFollowClasses = new HashSet<String>();
  static {
    noFollowClasses.add("java.lang.Boolean");
    noFollowClasses.add("java.lang.Byte");
    noFollowClasses.add("java.lang.Class");
    noFollowClasses.add("java.lang.Character");
    noFollowClasses.add("java.lang.Double");
    noFollowClasses.add("java.lang.Float");
    noFollowClasses.add("java.lang.Integer");
    noFollowClasses.add("java.lang.Long");
    noFollowClasses.add("java.lang.Object");
    noFollowClasses.add("java.lang.Short");
    noFollowClasses.add("java.lang.String");
  }

  private static boolean toFollow(Class oClass) {
    String name = oClass.getName();
    return !noFollowClasses.contains(name);
  }

  private static final Key<Boolean> REPORTED_LEAKED = Key.create("REPORTED_LEAKED");
  @TestOnly
  public static void checkProjectLeak(@Nonnull Object root) throws Exception {
    checkLeak(root, ProjectImpl.class);
  }
  @TestOnly
  public static void checkLeak(@Nonnull Object root, @Nonnull Class suspectClass) throws AssertionError {
    checkLeak(root, suspectClass, null);
  }
  @TestOnly
  public static <T> void checkLeak(@Nonnull Object root, @Nonnull Class<T> suspectClass, @javax.annotation.Nullable final Processor<T> isReallyLeak) throws AssertionError {
    if (SwingUtilities.isEventDispatchThread()) {
      UIUtil.dispatchAllInvocationEvents();
    }
    else {
      UIUtil.pump();
    }
    PersistentEnumerator.clearCacheForTests();
    toVisit.clear();
    visited.clear();
    toVisit.push(new BackLink(root.getClass(), root, null,null));
    try {
      walkObjects(suspectClass, new Processor<BackLink>() {
        @Override
        public boolean process(BackLink backLink) {
          UserDataHolder leaked = (UserDataHolder)backLink.value;
          if (((UserDataHolderBase)leaked).replace(REPORTED_LEAKED,null,Boolean.TRUE) && (isReallyLeak == null || isReallyLeak.process((T)leaked))) {
            String place = leaked instanceof Project ? PlatformTestCase.getCreationPlace((Project)leaked) : "";
            System.out.println("Leaked object found:" + leaked +
                               "; hash: "+System.identityHashCode(leaked) + "; place: "+ place);
            while (backLink != null) {
              String valueStr;
              try {
                valueStr = String.valueOf(backLink.value);
              }
              catch (Throwable e) {
                valueStr = "("+e.getMessage()+" while computing .toString())";
              }
              System.out.println("-->"+backLink.field+"; Value: "+ valueStr +"; "+backLink.aClass);
              backLink = backLink.backLink;
            }
            System.out.println(";-----");

            throw new AssertionError();
          }
          return true;
        }
      });
    }
    finally {
      visited.clear();
      toVisit.clear();
      toVisit.trimToSize();
    }
  }
}
