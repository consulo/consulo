/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.wm.WindowManager;
import consulo.actionSystem.ex.TopApplicationMenuUtil;
import consulo.ide.actions.AboutManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.Window;

import javax.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class AboutAction extends AnAction implements DumbAware {
  private final Provider<AboutManager> myAboutManager;
  private final Provider<WindowManager> myWindowManager;

  @Inject
  public AboutAction(Provider<AboutManager> aboutManager, Provider<WindowManager> windowManager) {
    myAboutManager = aboutManager;
    myWindowManager = windowManager;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent e) {
    e.getPresentation().setVisible(!TopApplicationMenuUtil.isMacSystemMenu);
    e.getPresentation().setDescription("Show information about " + ApplicationNamesInfo.getInstance().getFullProductName());
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    Window window = myWindowManager.get().suggestParentWindow(e.getData(CommonDataKeys.PROJECT));

    myAboutManager.get().showAbout(window);
  }
}
