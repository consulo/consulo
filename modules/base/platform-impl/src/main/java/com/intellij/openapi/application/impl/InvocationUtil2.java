// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.application.impl;

import consulo.awt.hacking.InvocationUtil;

import javax.annotation.Nonnull;

public class InvocationUtil2 extends InvocationUtil {
  @Nonnull
  public static final Class<? extends Runnable> FLUSH_NOW_CLASS = FlushQueue.FlushNow.class;
}
