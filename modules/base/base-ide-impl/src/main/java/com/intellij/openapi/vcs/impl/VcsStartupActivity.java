// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.impl;

import consulo.application.Application;
import com.intellij.openapi.project.Project;
import consulo.component.extension.ExtensionType;
import consulo.component.extension.ExtensionList;

import javax.annotation.Nonnull;

@ExtensionType(value = "vcsStartupActivity", component = Application.class)
public interface VcsStartupActivity {
  ExtensionList<VcsStartupActivity, Application> EP = ExtensionList.of(VcsStartupActivity.class);

  void runActivity(@Nonnull Project project);

  /**
   * @see VcsInitObject#getOrder()
   */
  int getOrder();
}