/*
 * Copyright 2013-2019 consulo.io
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
package com.intellij.execution.console;

import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorLinePainter;
import com.intellij.openapi.editor.LineExtensionInfo;
import com.intellij.openapi.editor.TextAnnotationGutterProvider;
import com.intellij.openapi.editor.colors.EditorColorKey;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * from kotlin
 */
public class ConsolePromptDecorator extends EditorLinePainter implements TextAnnotationGutterProvider {
  private static final EditorColorKey promptColor = EditorColorKey.createColorKey("CONSOLE_PROMPT_COLOR");

  private final EditorEx myEditorEx;

  private ConsoleViewContentType promptAttributes = ConsoleViewContentType.USER_INPUT;

  private String mainPrompt = ">";

  private String indentPrompt = "";

  public ConsolePromptDecorator(EditorEx editorEx) {
    myEditorEx = editorEx;

    myEditorEx.getColorsScheme().setColor(promptColor, promptAttributes.getAttributes().getForegroundColor());
  }

  public void setPromptAttributes(ConsoleViewContentType promptAttributes) {
    this.promptAttributes = promptAttributes;

    myEditorEx.getColorsScheme().setColor(promptColor, promptAttributes.getAttributes().getForegroundColor());

    update();
  }

  public void setMainPrompt(String mainPrompt) {
    if(!StringUtil.equals(this.mainPrompt, mainPrompt)) {
      // to be compatible with LanguageConsoleView we should reset the indent prompt
      this.indentPrompt = "";
      this.mainPrompt = mainPrompt;
      update();
    }
  }

  public String getMainPrompt() {
    return mainPrompt;
  }

  public void setIndentPrompt(String indentPrompt) {
    this.indentPrompt = indentPrompt;
    update();
  }

  public String getIndentPrompt() {
    return myEditorEx.isRendererMode() ? "" : indentPrompt;
  }

  public ConsoleViewContentType getPromptAttributes() {
    return promptAttributes;
  }

  @Nullable
  @Override
  public Collection<LineExtensionInfo> getLineExtensions(@Nonnull Project project, @Nonnull VirtualFile file, int lineNumber) {
    return null;
  }

  @Nullable
  @Override
  public String getLineText(int line, Editor editor) {
    if(line == 0) {
      return mainPrompt;
    }
    else if(line > 0) {
      return getIndentPrompt();
    }
    return null;
  }

  @Nullable
  @Override
  public String getToolTip(int line, Editor editor) {
    return null;
  }

  @Override
  public EditorFontType getStyle(int line, Editor editor) {
    return EditorFontType.CONSOLE_PLAIN;
  }

  @Nullable
  @Override
  public EditorColorKey getColor(int line, Editor editor) {
    return promptColor;
  }

  @Nullable
  @Override
  public ColorValue getBgColor(int line, Editor editor) {
    ColorValue backgroundColor = this.promptAttributes.getAttributes().getBackgroundColor();
    if(backgroundColor == null) {
      backgroundColor = myEditorEx.getBackgroundColor();
    }
    return backgroundColor;
  }

  @Override
  public boolean useMargin() {
    return false;
  }

  @Override
  public List<AnAction> getPopupActions(int line, Editor editor) {
    return null;
  }

  @Override
  public void gutterClosed() {

  }

  public void update() {
    UIUtil.invokeLaterIfNeeded(() -> myEditorEx.getGutterComponentEx().revalidateMarkup());
  }
}
