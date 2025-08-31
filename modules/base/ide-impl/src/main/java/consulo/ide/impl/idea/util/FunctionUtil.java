/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import org.jetbrains.annotations.Contract;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Function;

/**
 * @author nik
 */
public class FunctionUtil {
  private FunctionUtil() { }

  @Nonnull
  public static <T> Function<T, T> id() {
    return Function.identity();
  }

  @Nonnull
  public static <A, B> NullableFunction<A, B> nullConstant() {
    @SuppressWarnings("unchecked") NullableFunction<A, B> function = NullableFunction.NULL;
    return function;
  }

  @Nonnull
  public static <T> Function<T, String> string() {
    return Object::toString;
  }

  @Nonnull
  public static <A, B> Function<A, B> constant(B b) {
    return a -> b;
  }

  @Nonnull
  public static <A, B, C> NotNullFunction<A, C> composition(@Nonnull final NotNullFunction<B, C> f, @Nonnull final NotNullFunction<A, B> g) {
    return new NotNullFunction<A, C>() {
      @Override
      @Nonnull
      public C apply(A a) {
        return f.apply(g.apply(a));
      }
    };
  }

  /**
   * Returns a runnable which runs both supplied runnables. If any of them throws, the second one is still executed.
   * If both throw, the second exception is added to the first one as suppressed.
   *
   * @param r1 first runnable to run
   * @param r2 second runnable to run
   * @return composed runnable. If one of arguments is null, returns other argument.
   */
  @Contract(value = "_, null -> param1; null, !null -> param2", pure = true)
  public static Runnable composeRunnables(@Nullable Runnable r1, @Nullable Runnable r2) {
    if (r2 == null) return r1;
    if (r1 == null) return r2;
    return () -> {
      try {
        r1.run();
      }
      catch (RuntimeException | Error ex) {
        try {
          r2.run();
        }
        catch (RuntimeException | Error ex2) {
          ex.addSuppressed(ex2);
        }
        throw ex;
      }
      r2.run();
    };
  }
}
