/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.help.HelpManager;
import com.intellij.openapi.project.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class HelpTopicsAction extends AnAction implements DumbAware {
  private final Provider<ApplicationInfo> myInfoProvider;

  @Inject
  public HelpTopicsAction(Provider<ApplicationInfo> infoProvider) {
    myInfoProvider = infoProvider;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    HelpManager.getInstance().invokeHelp("top");
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    super.update(e);
    
    boolean enabled = myInfoProvider.get().hasHelp();
    e.getPresentation().setEnabled(enabled);
    e.getPresentation().setVisible(enabled);
  }
}
