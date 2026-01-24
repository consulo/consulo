// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream;

import consulo.logging.attachment.AttachmentFactory;
import consulo.logging.attachment.RuntimeExceptionWithAttachments;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class TraceException extends RuntimeExceptionWithAttachments {
  TraceException(@Nonnull String message, @Nonnull String traceExpression) {
    super(message, AttachmentFactory.get().create("trace.txt", traceExpression));
  }
}
