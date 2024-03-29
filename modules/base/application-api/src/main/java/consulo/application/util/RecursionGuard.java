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
package consulo.application.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * A helper object for {@link RecursionManager}. It's obtained from {@link RecursionManager#createGuard(String)}.
 *
 * @author peter
 */
public abstract class RecursionGuard<Key> {

  /**
   * Run the given computation, unless it's already running in this thread.
   *
   * @param key         an id of the computation. It's stored internally to ensure that a recursive calls with the same key won't lead to endless recursion.
   * @param memoize     whether the result of the computation may be cached thread-locally until the last currently active {@code doPreventingRecursion} call
   *                    completes. May be used to speed up things when recursion re-entrance happens: otherwise nothing would be cached at all and
   *                    in some cases exponential performance may be observed. Pass {@code true} if your computation has no side effects
   *                    and doesn't depend on method parameters.
   * @param computation a piece of code to compute.
   * @return the result of the computation or {@code null} if we're entering a computation with this key on this thread recursively,
   */
  @Nullable
  public abstract <T> T doPreventingRecursion(@Nonnull Key key, boolean memoize, @Nonnull Supplier<T> computation);

  /**
   * @deprecated Use {@link RecursionManager#markStack()} instead
   */
  @Nonnull
  @Deprecated
  public StackStamp markStack() {
    return RecursionManager.markStack();
  }

  /**
   * Note: if you make decisions based on the result of this method, you'd better couple it with {@link #prohibitResultCaching},
   * otherwise you might cache inconsistent values.
   *
   * @return the current thread-local stack of keys passed to {@link #doPreventingRecursion(Key, boolean, Supplier)}.
   */
  @Nonnull
  public abstract List<? extends Key> currentStack();

  /**
   * Makes {@link RecursionGuard.StackStamp#mayCacheNow()} return false for all stamps created since a computation with
   * key {@code since} began.<p></p>
   * <p>
   * Used to prevent caching of results that are non-reliable NOT due to recursion prevention: for example, too deep recursion
   * ({@link #currentStack()} may help in determining the recursion depth).<p></p>
   * <p>
   * Also disables thread-local memoization (see the second parameter of {@link #doPreventingRecursion}.
   *
   * @param since the id of a computation whose result is safe to cache whilst for more nested ones it's not.
   */
  public abstract void prohibitResultCaching(@Nonnull Key since);

  public interface StackStamp {

    /**
     * @return whether a computation that started at the moment of this {@link StackStamp} instance creation does not depend on any
     * re-entrant recursive results. When such non-reliable results exist in the thread's call stack, returns false, otherwise true.<p></p>
     * <p>
     * If you use this with {@link #doPreventingRecursion}, then the
     * {@link RecursionManager#markStack()}+{@link #mayCacheNow()} should be outside of recursion prevention call. Otherwise
     * even the outer recursive computation result won't be cached. In particular, {@code doPreventingRecursion} calls should
     * be inside your {@link CachedValue} provider, not outside cached value access.
     */
    @SuppressWarnings("JavaDoc")
    boolean mayCacheNow();
  }
}
