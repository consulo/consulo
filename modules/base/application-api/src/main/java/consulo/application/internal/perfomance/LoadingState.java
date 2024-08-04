// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.internal.perfomance;

import consulo.util.lang.Comparing;
import consulo.logging.Logger;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;

import jakarta.annotation.Nonnull;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum LoadingState {
  BOOTSTRAP("bootstrap"),
  LAF_INITIALIZED("LaF is initialized"),
  COMPONENTS_REGISTERED("app component registered"),
  CONFIGURATION_STORE_INITIALIZED("app store initialized"),
  COMPONENTS_LOADED("app component loaded"),
  PROJECT_OPENED("project opened"),
  INDEXING_FINISHED("indexing finished");

  final String displayName;

  private static boolean CHECK_LOADING_PHASE;

  LoadingState(@Nonnull String displayName) {
    this.displayName = displayName;
  }

  @Nonnull
  static Logger getLogger() {
    return Logger.getInstance(LoadingState.class);
  }

  //@ApiStatus.Internal
  public static void setStrictMode() {
    CHECK_LOADING_PHASE = true;
  }

  private final static Set<Throwable> stackTraces = Sets.newHashSet(new HashingStrategy<Throwable>() {
    @Override
    public int hashCode(Throwable throwable) {
      return getCollect(throwable).hashCode();
    }

    private String getCollect(Throwable throwable) {
      return Arrays.stream(throwable.getStackTrace()).map(element -> element.getClassName() + element.getMethodName()).collect(Collectors.joining());
    }

    @Override
    public boolean equals(Throwable o1, Throwable o2) {
      if (o1 == o2) return true;
      if (o1 == null || o2 == null) return false;
      return Comparing.equal(getCollect(o1), getCollect(o2));
    }
  });

  public void checkOccurred() {
    if (!CHECK_LOADING_PHASE) {
      return;
    }

    LoadingState currentState = StartUpMeasurer.currentState.get();
    if (currentState.ordinal() >= ordinal() || isKnownViolator()) {
      return;
    }

    Throwable t = new Throwable();
    synchronized (stackTraces) {
      if (!stackTraces.add(t)) {
        return;
      }

      getLogger().error("Should be called at least in the state " + this + ", the current state is: " + currentState + "\n" + "Current violators count: " + stackTraces.size() + "\n\n", t);
    }
  }

  private static boolean isKnownViolator() {
    for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
      String className = element.getClassName();
      if (className.contains("consulo.ide.impl.idea.util.indexing.IndexInfrastructure") || className.contains("com.intellij.psi.impl.search.IndexPatternSearcher")) {
        return true;
      }
    }
    return false;
  }

  public boolean isOccurred() {
    return StartUpMeasurer.currentState.get().ordinal() >= ordinal();
  }
}