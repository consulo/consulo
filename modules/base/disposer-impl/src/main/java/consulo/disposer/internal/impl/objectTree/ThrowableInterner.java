// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.disposer.internal.impl.objectTree;

import consulo.hacking.java.base.ThrowableHacking;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.interner.Interner;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

/**
 * Please don't look, there's nothing interesting here.
 * <p>
 * <p>
 * <p>
 * <p>
 * If you insist, JVM stores stacktrace information in compact form in Throwable.backtrace field.
 * This class uses this field for comparing Throwables.
 * The available method Throwable.getStackTrace() unfortunately can't be used for that because it's
 * 1) too slow and 2) explodes Throwable retained size by polluting Throwable.stackTrace fields.
 */
public class ThrowableInterner {
  private static final Interner<Throwable> ourTraceInterner = Interner.createWeakInterner(new HashingStrategy<Throwable>() {
    @Override
    public int hashCode(Throwable throwable) {
      String message = throwable.getMessage();
      if (message != null) {
        return message.hashCode();
      }
      Object[] backtrace = getBacktrace(throwable);
      if (backtrace != null) {
        Object[] stack = (Object[])ContainerUtil.find(backtrace, it -> it instanceof Object[]);
        return Arrays.hashCode(stack);
      }
      return Arrays.hashCode(throwable.getStackTrace());
    }

    @Override
    public boolean equals(Throwable o1, Throwable o2) {
      if (o1 == o2) return true;
      if (o1 == null || o2 == null) return false;

      if (!Objects.equals(o1.getClass(), o2.getClass())) return false;
      if (!Objects.equals(o1.getMessage(), o2.getMessage())) return false;
      if (!equals(o1.getCause(), o2.getCause())) return false;
      Object[] backtrace1 = getBacktrace(o1);
      Object[] backtrace2 = getBacktrace(o2);
      if (backtrace1 != null && backtrace2 != null) {
        return Arrays.deepEquals(backtrace1, backtrace2);
      }
      return Arrays.equals(o1.getStackTrace(), o2.getStackTrace());
    }
  });

  private static Function<Throwable, Object> ourBacktraceAccess = ThrowableHacking.getBacktraceAccess();

  private static Object[] getBacktrace(@Nonnull Throwable throwable) {
    return ourBacktraceAccess == null ? null : (Object[])ourBacktraceAccess.apply(throwable);
  }

  @Nonnull
  public static Throwable intern(@Nonnull Throwable throwable) {
    return getBacktrace(throwable) == null ? throwable : ourTraceInterner.intern(throwable);
  }
}
