// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.internal.psi;

import consulo.application.util.function.ThrowableComputable;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Enforces tree loading policies.
 * <p/>
 * For example it's very slow to load AST for all shown files when updating Project View nodes,
 * or loading other files when current file is being highlighted.
 * <p/>
 * To prevent loading <i>in the current thread</i> {@link #disallowTreeLoading} should be used.<br/>
 * In this case the exception will be thrown when some code unexpectedly tries to load the tree,
 * and then there are two options:
 * <ul>
 * <li>examine the stack trace, fix the code, so tree loading won't occur anymore,
 * e.g. data will be taken from stubs, and cover it with a test.
 * <b>It is highly preferable to fix the code</b>, which gains a speed up.
 * </li>
 * <li>force allow tree loading by wrapping troublesome operation into {@link #forceAllowTreeLoading}.
 * In this case there are no performance gains, but later it will be possible to examine all bottlenecks
 * by finding usages of {@link #forceAllowTreeLoading}</li>
 * </ul>
 * Example:
 * <pre>
 * disallowTreeLoading {
 *   // some deep trace
 *   ...
 *   doSomeOperation {
 *     // access file tree, exception is thrown
 *   }
 *   forceAllowTreeLoading(file) {
 *     doSomeOperation {
 *       // access file tree, no exception is thrown
 *     }
 *     doAnotherOperation {
 *       // access another file tree, exception is thrown
 *     }
 *   }
 *   forceAllowTreeLoading(file) {
 *     disallowTreeLoading {
 *       doSomeOperation {
 *         // nested disallowing has no effect in any case
 *         // access file tree, no exception is thrown, access is still allowed for file
 *       }
 *     }
 *   }
 * }
 * </pre>
 * <p/>
 * Note that tree access won't result in an exception when the tree was already loaded.
 */
public class AstLoadingFilter {

  private static final Logger LOG = Logger.getInstance(AstLoadingFilter.class);
  //TODO private static final FrequentErrorLogger ourErrorLogger = FrequentErrorLogger.newInstance(LOG);
  /**
   * Holds not-null value if loading was disabled in current thread.
   * Initial value is {@code null} meaning loading is enabled by default.
   */
  private static final ThreadLocal<Supplier<String>> myDisallowedInfo = new ThreadLocal<>();
  @SuppressWarnings("SSBasedInspection")
  private static final ThreadLocal<Set<VirtualFile>> myForcedAllowedFiles = ThreadLocal.withInitial(() -> new HashSet<>());

  private static final boolean AST_LOADING_FILTER = Boolean.getBoolean("consulo.ast.loading.filter");

  private AstLoadingFilter() {
  }

  public static void assertTreeLoadingAllowed(@Nonnull VirtualFile file) {
    if (!AST_LOADING_FILTER) return;
    if (file instanceof VirtualFileWindow) return;
    Supplier<String> disallowedInfo = myDisallowedInfo.get();
    if (disallowedInfo == null) {
      // loading was not disabled in current thread
    }
    else if (myForcedAllowedFiles.get().contains(file)) {
      // loading was disabled but then re-enabled for file
    }
    else {
      AstLoadingException throwable = new AstLoadingException();
      // TODO ourErrorLogger.error("Tree access disabled", throwable, AttachmentFactory.get().create("debugInfo", buildDebugInfo(file, disallowedInfo)));
    }
  }

  @Nonnull
  private static String buildDebugInfo(@Nonnull VirtualFile file, @Nonnull Supplier<String> disabledInfo) {
    StringBuilder debugInfo = new StringBuilder();
    debugInfo.append("Accessed file path: ").append(file.getPath());
    String additionalInfo = disabledInfo.get();
    if (additionalInfo != null) {
      debugInfo.append('\n').append("Additional info: \n").append(additionalInfo);
    }
    return debugInfo.toString();
  }

  public static <E extends Throwable> void disallowTreeLoading(@Nonnull ThrowableRunnable<E> runnable) throws E {
    disallowTreeLoading(toComputable(runnable));
  }

  public static <T, E extends Throwable> T disallowTreeLoading(@Nonnull ThrowableComputable<? extends T, E> computable) throws E {
    return disallowTreeLoading(computable, () -> null);
  }

  public static <T, E extends Throwable> T disallowTreeLoading(@Nonnull ThrowableComputable<? extends T, E> computable, @Nonnull Supplier<String> debugInfo) throws E {
    if (myDisallowedInfo.get() != null) {
      return computable.compute();
    }
    else {
      try {
        myDisallowedInfo.set(debugInfo);
        return computable.compute();
      }
      finally {
        myDisallowedInfo.set(null);
      }
    }
  }

  public static <E extends Throwable> void forceAllowTreeLoading(@Nullable PsiFile psiFile, @Nonnull ThrowableRunnable<E> runnable) throws E {
    forceAllowTreeLoading(psiFile, toComputable(runnable));
  }

  public static <E extends Throwable> void forceAllowTreeLoading(@Nonnull VirtualFile virtualFile, @Nonnull ThrowableRunnable<E> runnable) throws E {
    forceAllowTreeLoading(virtualFile, toComputable(runnable));
  }

  public static <T, E extends Throwable> T forceAllowTreeLoading(@Nullable PsiFile psiFile, @Nonnull ThrowableComputable<? extends T, E> computable) throws E {
    VirtualFile virtualFile = psiFile == null ? null : psiFile.getVirtualFile();
    return virtualFile == null ? computable.compute() : forceAllowTreeLoading(virtualFile, computable);
  }

  public static <T, E extends Throwable> T forceAllowTreeLoading(@Nonnull VirtualFile virtualFile, @Nonnull ThrowableComputable<? extends T, E> computable) throws E {
    Set<VirtualFile> enabledFiles = myForcedAllowedFiles.get();
    if (enabledFiles.add(virtualFile)) {
      try {
        return computable.compute();
      }
      finally {
        enabledFiles.remove(virtualFile);
      }
    }
    else {
      return computable.compute();
    }
  }

  private static <E extends Throwable> ThrowableComputable<?, E> toComputable(ThrowableRunnable<? extends E> runnable) {
    return () -> {
      runnable.run();
      return null;
    };
  }
}
