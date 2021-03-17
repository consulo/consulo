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
package consulo.disposer.internal.impl;

import consulo.disposer.internal.impl.objectTree.ObjectTree;
import com.intellij.util.containers.ContainerUtil;
import consulo.disposer.Disposable;
import consulo.disposer.TraceableDisposable;
import consulo.disposer.internal.DisposerInternal;
import consulo.util.lang.ObjectUtil;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public class DisposerInternalImpl extends DisposerInternal {
  private final ObjectTree myTree;

  public DisposerInternalImpl() {
    myTree = new ObjectTree();
  }

  private final Map<String, Disposable> myKeyDisposables = ContainerUtil.createConcurrentWeakMap();

  private final static String debugDisposer = System.getProperty("idea.disposer.debug");

  public static boolean isDebugDisposerOn() {
    return "on".equals(debugDisposer);
  }

  private static boolean ourDebugMode;

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

  public void register(@Nonnull Disposable parent, @Nonnull Disposable child) {
    register(parent, child, null);
  }

  @Override
  public void register(@Nonnull Disposable parent, @Nonnull Disposable child, @NonNls @Nullable final String key) {
    myTree.register(parent, child);

    if (key != null) {
      Disposable v = get(key);
      if (v != null) throw new IllegalArgumentException("Key " + key + " already registered: " + v);
      myKeyDisposables.put(key, child);

      register(child, () -> myKeyDisposables.remove(key));
    }
  }

  @Override
  public boolean isDisposed(@Nonnull Disposable disposable) {
    return myTree.getDisposalInfo(disposable) != null;
  }

  @Override
  public boolean isDisposing(@Nonnull Disposable disposable) {
    return myTree.isDisposing(disposable);
  }

  @Override
  public Disposable get(@Nonnull String key) {
    return myKeyDisposables.get(key);
  }

  @Override
  public Throwable getDisposalTrace(@Nonnull Disposable disposable) {
    return ObjectUtil.tryCast(getTree().getDisposalInfo(disposable), Throwable.class);
  }

  public void dispose(@Nonnull Disposable disposable) {
    dispose(disposable, true);
  }

  @Override
  public void dispose(@Nonnull Disposable disposable, boolean processUnregistered) {
    myTree.executeAll(disposable, processUnregistered);
  }

  @Override
  public TraceableDisposable newTraceDisposable(boolean debug) {
    return new TraceableDisposableImpl(debug);
  }

  @Nonnull
  public ObjectTree getTree() {
    return myTree;
  }

  @Override
  public void assertIsEmpty() {
    assertIsEmpty(false);
  }

  public void assertIsEmpty(boolean throwError) {
    if (ourDebugMode) {
      myTree.assertIsEmpty(throwError);
    }
  }

  /**
   * @return old value
   */
  @Override
  public boolean setDebugMode(boolean debugMode) {
    if (debugMode) {
      debugMode = !"off".equals(debugDisposer);
    }
    boolean oldValue = ourDebugMode;
    ourDebugMode = debugMode;
    return oldValue;
  }

  @Override
  public boolean isDebugMode() {
    return ourDebugMode;
  }

  /**
   * @return object registered on parentDisposable which is equal to object, or null if not found
   */
  @Override
  @Nullable
  public <T extends Disposable> T findRegisteredObject(@Nonnull Disposable parentDisposable, @Nonnull T object) {
    return myTree.findRegisteredObject(parentDisposable, object);
  }
}
