// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
// Copyright 2013-2026 consulo.io
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.application.progress.ProgressIndicator;

import java.util.concurrent.Future;

/**
 * @author peter
 */
interface CompletionThreading {

    Future<?> startThread(ProgressIndicator progressIndicator, Runnable runnable);

    WeighingDelegate delegateWeighing(CompletionProgressIndicator indicator);
}
