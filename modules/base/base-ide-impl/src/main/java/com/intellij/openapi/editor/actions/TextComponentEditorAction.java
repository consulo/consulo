/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.editor.actions;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.textarea.TextComponentEditorImpl;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.text.JTextComponent;

/**
 * @author yole
 */
public abstract class TextComponentEditorAction extends EditorAction {
  protected TextComponentEditorAction(@Nonnull EditorActionHandler defaultHandler) {
    super(defaultHandler);
  }

  @Override
  @Nullable
  protected Editor getEditor(@Nonnull final DataContext dataContext) {
    return getEditorFromContext(dataContext);
  }

  @Nullable
  public static Editor getEditorFromContext(@Nonnull DataContext dataContext) {
    final Editor editor = dataContext.getData(CommonDataKeys.EDITOR);
    if (editor != null) return editor;
    final Object data = dataContext.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    if (data instanceof JTextComponent) {
      return new TextComponentEditorImpl(dataContext.getData(CommonDataKeys.PROJECT), (JTextComponent) data);
    }
    return null;
  }
}