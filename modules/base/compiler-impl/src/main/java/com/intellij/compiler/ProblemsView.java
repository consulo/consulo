/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.compiler;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.pom.Navigatable;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ArrayUtil;
import consulo.ui.annotation.RequiredUIAccess;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Eugene Zhuravlev
 * Date: 9/18/12
 */
public abstract class ProblemsView {
  @Nonnull
  public static ProblemsView getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, ProblemsView.class);
  }

  public static void showCurrentFileProblems(@Nonnull Project project) {
    ToolWindow window = getToolWindow(project);
    if (window == null) return; // does not exist
    selectContent(window.getContentManager(), 0);
    window.setAvailable(true, null);
    window.activate(null, true);
  }

  @Nullable
  public static ToolWindow getToolWindow(@Nullable Project project) {
    return project == null || project.isDisposed() ? null : ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
  }

  private static void selectContent(@Nonnull ContentManager manager, int index) {
    Content content = manager.getContent(index);
    if (content != null) manager.setSelectedContent(content);
  }

  public abstract void clearOldMessages();

  public abstract void addMessage(int type,
                                  @Nonnull String[] text,
                                  @Nullable String groupName,
                                  @Nullable Navigatable navigatable,
                                  @Nullable String exportTextPrefix,
                                  @Nullable String rendererTextPrefix);


  @RequiredUIAccess
  public abstract void showOrHide(boolean hide);

  public abstract boolean isHideWarnings();

  public abstract void selectFirstMessage();

  public abstract void setProgress(String text, float fraction);

  public abstract void setProgress(String text);

  public abstract void clearProgress();

  public static String[] convertMessage(final String text) {
    if (!text.contains("\n")) {
      return new String[]{text};
    }
    final List<String> lines = new ArrayList<>();
    StringTokenizer tokenizer = new StringTokenizer(text, "\n", false);
    while (tokenizer.hasMoreTokens()) {
      lines.add(tokenizer.nextToken());
    }
    return ArrayUtil.toStringArray(lines);
  }
}
