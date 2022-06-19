// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.vcs.impl;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.application.Application;
import consulo.component.extension.ExtensionList;
import consulo.project.Project;

import javax.annotation.Nonnull;

@Extension(ComponentScope.APPLICATION)
public interface VcsStartupActivity {
  ExtensionList<VcsStartupActivity, Application> EP = ExtensionList.of(VcsStartupActivity.class);

  void runActivity(@Nonnull Project project);

  /**
   * @see VcsInitObject#getOrder()
   */
  int getOrder();
}