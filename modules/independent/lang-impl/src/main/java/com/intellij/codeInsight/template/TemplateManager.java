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

package com.intellij.codeInsight.template;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.util.PairProcessor;
import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Map;

public abstract class TemplateManager {
  public static TemplateManager getInstance(Project project) {
    return ServiceManager.getService(project, TemplateManager.class);
  }

  public abstract void startTemplate(@Nonnull Editor editor, @Nonnull Template template);

  public abstract void startTemplate(@Nonnull Editor editor, String selectionString, @Nonnull Template template);

  public abstract void startTemplate(@Nonnull Editor editor, @Nonnull Template template, TemplateEditingListener listener);

  public abstract void startTemplate(@Nonnull final Editor editor,
                                     @Nonnull final Template template,
                                     boolean inSeparateCommand,
                                     Map<String, String> predefinedVarValues,
                                     @Nullable TemplateEditingListener listener);

  public abstract void startTemplate(@Nonnull Editor editor,
                                     @Nonnull Template template,
                                     TemplateEditingListener listener,
                                     final PairProcessor<String, String> callback);

  public abstract boolean startTemplate(@Nonnull Editor editor, char shortcutChar);

  public abstract Template createTemplate(@Nonnull String key, String group);

  public abstract Template createTemplate(@Nonnull String key, String group, @NonNls String text);

  @Nullable
  public abstract Template getActiveTemplate(@Nonnull Editor editor);

  /**
   * Finished a live template in the given editor, if it's present
   * @return whether a live template was present
   */
  public abstract boolean finishTemplate(@Nonnull Editor editor);
}
