// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.compiler.impl.internal;

import consulo.compiler.ExitStatus;
import consulo.compiler.CompilerMessage;
import consulo.application.progress.ProgressIndicator;

import jakarta.annotation.Nullable;

/**
 * {@link BuildViewService} implementations are expected to visualize somehow compiler progress/messages for {@link CompilerTask}.
 *
 * @see CompilerTask
 */
public interface BuildViewService {
    void onStart(Object sessionId, long startCompilationStamp, @Nullable Runnable restartWork, ProgressIndicator indicator);

    void onEnd(Object sessionId, ExitStatus exitStatus, long endCompilationStamp);

    void addMessage(Object sessionId, CompilerMessage message);

    void onProgressChange(Object sessionId, ProgressIndicator indicator);

    void registerCloseAction(Runnable onClose);
}
