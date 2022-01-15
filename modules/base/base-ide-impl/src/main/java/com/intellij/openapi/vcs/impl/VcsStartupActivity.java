// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.impl;

import com.intellij.openapi.project.Project;
import javax.annotation.Nonnull;

public interface VcsStartupActivity {
  void runActivity(@Nonnull Project project);

  /**
   * @see VcsInitObject#getOrder()
   */
  int getOrder();
}