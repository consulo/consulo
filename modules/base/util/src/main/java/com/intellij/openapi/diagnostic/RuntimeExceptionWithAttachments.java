// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.diagnostic;

import consulo.annotation.DeprecationInfo;
import consulo.logging.attachment.Attachment;

import javax.annotation.Nullable;

/**
 * @author peter
 */
@SuppressWarnings("ExceptionClassNameDoesntEndWithException")
@Deprecated
@DeprecationInfo("Use consulo.logging.attachment.RuntimeExceptionWithAttachments")
public class RuntimeExceptionWithAttachments extends consulo.logging.attachment.RuntimeExceptionWithAttachments {
  public RuntimeExceptionWithAttachments(String message, Attachment... attachments) {
    super(message, attachments);
  }

  public RuntimeExceptionWithAttachments(Throwable cause, Attachment... attachments) {
    super(cause, attachments);
  }

  public RuntimeExceptionWithAttachments(String message, @Nullable Throwable cause, Attachment... attachments) {
    super(message, cause, attachments);
  }

  public RuntimeExceptionWithAttachments(String userMessage, String details, Attachment... attachments) {
    super(userMessage, details, attachments);
  }
}