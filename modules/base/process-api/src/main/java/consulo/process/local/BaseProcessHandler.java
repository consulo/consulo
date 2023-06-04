/*
 * Copyright 2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package consulo.process.local;

import consulo.annotation.DeprecationInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.Charset;

@Deprecated
@DeprecationInfo("Use consulo.process.BaseProcessHandler")
public abstract class BaseProcessHandler<T extends Process> extends consulo.process.BaseProcessHandler<T> {
  public BaseProcessHandler(@Nonnull T process, String commandLine, @Nullable Charset charset) {
    super(process, commandLine, charset);
  }
}