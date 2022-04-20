// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.usage.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.usage.Usage;
import consulo.usage.UsageView;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public interface UsageViewEx extends UsageView {
  boolean searchHasBeenCancelled();

  void cancelCurrentSearch();

  void associateProgress(@Nonnull ProgressIndicator indicator);

  void waitForUpdateRequestsCompletion();

  @Nonnull
  CompletableFuture<?> appendUsagesInBulk(@Nonnull Collection<? extends Usage> usages);

  void setSearchInProgress(boolean searchInProgress);

  void searchFinished();
}
