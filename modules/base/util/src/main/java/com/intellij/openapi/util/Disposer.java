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
package com.intellij.openapi.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.objectTree.ObjectTree;
import com.intellij.openapi.util.objectTree.ObjectTreeAction;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

public class Disposer {
  private static final ObjectTree<Disposable> ourTree;

  static {
    try {
      ourTree = new ObjectTree<Disposable>();
    }
    catch (NoClassDefFoundError e) {
      throw new RuntimeException("loader=" + Disposer.class.getClassLoader(), e);
    }
  }

  private static final ObjectTreeAction<Disposable> ourDisposeAction = new ObjectTreeAction<Disposable>() {
    @Override
    public void execute(@Nonnull final Disposable each) {
      //noinspection SSBasedInspection
      each.dispose();
    }

    @Override
    public void beforeTreeExecution(@Nonnull final Disposable parent) {
      if (parent instanceof Disposable.Parent) {
        ((Disposable.Parent)parent).beforeTreeDispose();
      }
    }
  };

  private final static String debugDisposer = System.getProperty("idea.disposer.debug");
  public static boolean isDebugDisposerOn() {
    return "on".equals(debugDisposer);
  }

  private static boolean ourDebugMode;

  private Disposer() {
  }

  @Nonnull
  public static Disposable newDisposable() {
    return newDisposable(null);
  }

  @Nonnull
  public static Disposable newDisposable(@Nullable final String debugName) {
    return new Disposable() {
      @Override
      public void dispose() {
      }

      @Override
      public String toString() {
        return debugName == null ? super.toString() : debugName;
      }
    };
  }

  private static final Map<String, Disposable> ourKeyDisposables = ContainerUtil.createConcurrentWeakMap();

  public static void register(@Nonnull Disposable parent, @Nonnull Disposable child) {
    register(parent, child, null);
  }

  public static void register(@Nonnull Disposable parent, @Nonnull Disposable child, @NonNls @Nullable final String key) {
    ourTree.register(parent, child);

    if (key != null) {
      Disposable v = get(key);
      if (v != null) throw new IllegalArgumentException("Key "+key+" already registered: "+v);
      ourKeyDisposables.put(key, child);
      register(child, new Disposable() {
        @Override
        public void dispose() {
          ourKeyDisposables.remove(key);
        }
      });
    }
  }

  public static boolean isDisposed(@Nonnull Disposable disposable) {
    return ourTree.getDisposalInfo(disposable) != null;
  }

  public static Disposable get(@Nonnull String key) {
    return ourKeyDisposables.get(key);
  }

  public static void dispose(@Nonnull Disposable disposable) {
    dispose(disposable, true);
  }

  public static void dispose(@Nonnull Disposable disposable, boolean processUnregistered) {
    ourTree.executeAll(disposable, ourDisposeAction, processUnregistered);
  }

  public static void disposeChildAndReplace(@Nonnull Disposable toDispose, @Nonnull Disposable toReplace) {
    ourTree.executeChildAndReplace(toDispose, toReplace, ourDisposeAction);
  }

  @Nonnull
  public static ObjectTree<Disposable> getTree() {
    return ourTree;
  }

  public static void assertIsEmpty() {
    assertIsEmpty(false);
  }
  public static void assertIsEmpty(boolean throwError) {
    if (ourDebugMode) {
      ourTree.assertIsEmpty(throwError);
    }
  }

  @TestOnly
  public static boolean isEmpty() {
    return ourDebugMode && ourTree.isEmpty();
  }

  /**
   * @return old value
   */
  public static boolean setDebugMode(boolean debugMode) {
    if (debugMode) {
      debugMode = !"off".equals(debugDisposer);
    }
    boolean oldValue = ourDebugMode;
    ourDebugMode = debugMode;
    return oldValue;
  }

  public static boolean isDebugMode() {
    return ourDebugMode;
  }

  public static void clearOwnFields(@Nullable Object object, @Nonnull Condition<? super Field> selectCondition) {
    if (object == null) return;
    for (Field each : ReflectionUtil.collectFields(object.getClass())) {
      if ((each.getModifiers() & (Modifier.FINAL | Modifier.STATIC)) > 0) continue;
      if (!selectCondition.value(each)) continue;
      try {
        ReflectionUtil.resetField(object, each);
      }
      catch (Exception ignore) {
      }
    }
  }

  /**
   * @return object registered on parentDisposable which is equal to object, or null if not found
   */
  @Nullable
  public static <T extends Disposable> T findRegisteredObject(@Nonnull Disposable parentDisposable, @Nonnull T object) {
    return ourTree.findRegisteredObject(parentDisposable, object);
  }
}
