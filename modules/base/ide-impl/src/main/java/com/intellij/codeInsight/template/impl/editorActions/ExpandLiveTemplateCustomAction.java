/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.template.impl.editorActions;

import consulo.language.editor.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.action.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import consulo.project.Project;
import consulo.annotation.access.RequiredWriteAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
public class ExpandLiveTemplateCustomAction extends EditorAction {
  public ExpandLiveTemplateCustomAction() {
    super(createExpandTemplateHandler(TemplateSettings.CUSTOM_CHAR));
    setInjectedContext(true);
  }

  public static EditorWriteActionHandler createExpandTemplateHandler(final char shortcutChar) {
    return new EditorWriteActionHandler(true) {
      @RequiredWriteAction
      @Override
      public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        Project project = editor.getProject();
        assert project != null;
        TemplateManager.getInstance(project).startTemplate(editor, shortcutChar);
      }

      @Override
      protected boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
        Project project = editor.getProject();
        return project != null &&
               ((TemplateManagerImpl)TemplateManager.getInstance(project)).prepareTemplate(editor, shortcutChar, null) != null;
      }
    };
  }
}
