// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vfs.encoding;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.vfs.VirtualFile;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.nio.charset.Charset;

/**
 * @author yole
 */
public abstract class EncodingRegistry {
  public abstract boolean isNative2Ascii(@Nonnull VirtualFile virtualFile);

  public abstract boolean isNative2AsciiForPropertiesFiles();

  /**
   * @return charset configured in Settings|File Encodings|IDE encoding
   */
  @Nonnull
  public abstract Charset getDefaultCharset();

  /**
   * @param virtualFile       file to get encoding for
   * @param useParentDefaults true to determine encoding from the parent
   * @return encoding configured for this file in Settings|File Encodings or,
   * if useParentDefaults is true, encoding configured for nearest parent of virtualFile or,
   * null if there is no configured encoding found.
   */
  @Nullable
  public abstract Charset getEncoding(@Nullable VirtualFile virtualFile, boolean useParentDefaults);

  /**
   * @param virtualFileOrDir null means project
   * @param charset null means remove mapping
   */
  public abstract void setEncoding(@Nullable VirtualFile virtualFileOrDir, @Nullable Charset charset);

  // "null means 'use system-default'"
  @Nullable
  public Charset getDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile) {
    return null;
  }

  /**
   * @return encoding used by default in {@link com.intellij.execution.configurations.GeneralCommandLine}
   */
  @Nonnull
  public abstract Charset getDefaultConsoleEncoding();

  public static EncodingRegistry getInstance() {
    return EncodingManager.getInstance();
  }

  public static <E extends Throwable> VirtualFile doActionAndRestoreEncoding(@Nonnull VirtualFile fileBefore, @Nonnull ThrowableComputable<? extends VirtualFile, E> action) throws E {
    EncodingRegistry registry = getInstance();
    Charset charsetBefore = registry.getEncoding(fileBefore, true);
    VirtualFile fileAfter = null;
    try {
      fileAfter = action.compute();
      return fileAfter;
    }
    finally {
      if (fileAfter != null) {
        Charset actual = registry.getEncoding(fileAfter, true);
        if (!Comparing.equal(actual, charsetBefore)) {
          registry.setEncoding(fileAfter, charsetBefore);
        }
      }
    }
  }
}
