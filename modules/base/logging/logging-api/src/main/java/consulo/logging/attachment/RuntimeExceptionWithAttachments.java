// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.logging.attachment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author peter
 */
@SuppressWarnings("ExceptionClassNameDoesntEndWithException")
public class RuntimeExceptionWithAttachments extends RuntimeException implements ExceptionWithAttachments {
  private final String myUserMessage;
  private final Attachment[] myAttachments;

  public RuntimeExceptionWithAttachments(String message, Attachment... attachments) {
    super(message);
    myUserMessage = null;
    myAttachments = attachments;
  }

  public RuntimeExceptionWithAttachments(Throwable cause, Attachment... attachments) {
    super(cause);
    myUserMessage = null;
    myAttachments = attachments;
  }

  public RuntimeExceptionWithAttachments(String message, @Nullable Throwable cause, Attachment... attachments) {
    super(message, cause);
    myUserMessage = null;
    myAttachments = attachments;
  }

  /**
   * Corresponds to {@link Logger#error(String, Throwable, Attachment...)}
   * ({@code LOG.error(userMessage, new RuntimeException(details), attachments)}).
   */
  public RuntimeExceptionWithAttachments(String userMessage, String details, Attachment... attachments) {
    super(details);
    myUserMessage = userMessage;
    myAttachments = attachments;
  }

  @Nonnull
  public String getUserMessage() {
    return myUserMessage;
  }

  @Nonnull
  @Override
  public Attachment[] getAttachments() {
    return myAttachments;
  }
}