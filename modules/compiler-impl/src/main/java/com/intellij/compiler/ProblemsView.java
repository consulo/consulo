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

import com.intellij.compiler.progress.CompilerTask;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.util.ArrayUtil;
import consulo.annotations.RequiredDispatchThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * @author Eugene Zhuravlev
 *         Date: 9/18/12
 */
public abstract class ProblemsView {
  @NotNull
  public static ProblemsView getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, ProblemsView.class);
  }

  protected final Project myProject;

  protected ProblemsView(Project project) {
    myProject = project;
  }

  public abstract void clearOldMessages();

  public abstract void addMessage(int type,
                                  @NotNull String[] text,
                                  @Nullable String groupName,
                                  @Nullable Navigatable navigatable,
                                  @Nullable String exportTextPrefix,
                                  @Nullable String rendererTextPrefix);

  public final void addMessage(CompilerMessage message) {
    final VirtualFile file = message.getVirtualFile();
    Navigatable navigatable = message.getNavigatable();
    if (navigatable == null && file != null) {
      navigatable = new OpenFileDescriptor(myProject, file, -1, -1);
    }
    final CompilerMessageCategory category = message.getCategory();
    final int type = CompilerTask.translateCategory(category);
    final String[] text = convertMessage(message.getMessage());
    final String groupName = file != null? file.getPresentableUrl() : category.getPresentableText();
    addMessage(type, text, groupName, navigatable, message.getExportTextPrefix(), message.getRenderTextPrefix());
  }

  @RequiredDispatchThread
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
    final List<String> lines = new ArrayList<String>();
    StringTokenizer tokenizer = new StringTokenizer(text, "\n", false);
    while (tokenizer.hasMoreTokens()) {
      lines.add(tokenizer.nextToken());
    }
    return ArrayUtil.toStringArray(lines);
  }
  
}
