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

package com.intellij.psi.impl.source.codeStyle;

import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.ast.ASTNode;
import consulo.language.impl.psi.internal.IndentHelper;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.psi.impl.source.codeStyle.IndentHelperExtension;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

@Singleton
public final class IndentHelperImpl extends IndentHelper {
  //----------------------------------------------------------------------------------------------------

  public static final int INDENT_FACTOR = 10000; // "indent" is indent_level * INDENT_FACTOR + spaces

  @Override
  public int getIndent(@Nonnull PsiFile file, @Nonnull ASTNode element) {
    return getIndent(file, element, false);
  }

  @Override
  public int getIndent(@Nonnull PsiFile file, @Nonnull final ASTNode element, boolean includeNonSpace) {
    for (IndentHelperExtension extension : IndentHelperExtension.EP_NAME.getExtensionList()) {
      if (extension.isAvailable(file)) {
        return extension.getIndentInner(file, element, includeNonSpace, 0);
      }
    }
    return 0;
  }

  /**
   * @deprecated Use {@link #fillIndent(CommonCodeStyleSettings.IndentOptions, int)} instead.
   */
  @Deprecated
  public static String fillIndent(Project project, FileType fileType, int indent) {
    return fillIndent(CodeStyle.getProjectOrDefaultSettings(project).getIndentOptions(fileType), indent);
  }

  public static String fillIndent(@Nonnull CommonCodeStyleSettings.IndentOptions indentOptions, int indent) {
    int indentLevel = (indent + INDENT_FACTOR / 2) / INDENT_FACTOR;
    int spaceCount = indent - indentLevel * INDENT_FACTOR;
    int indentLevelSize = indentLevel * indentOptions.INDENT_SIZE;
    int totalSize = indentLevelSize + spaceCount;

    StringBuilder buffer = new StringBuilder();
    if (indentOptions.USE_TAB_CHARACTER) {
      if (indentOptions.SMART_TABS) {
        int tabCount = indentLevelSize / indentOptions.TAB_SIZE;
        int leftSpaces = indentLevelSize - tabCount * indentOptions.TAB_SIZE;
        for (int i = 0; i < tabCount; i++) {
          buffer.append('\t');
        }
        for (int i = 0; i < leftSpaces + spaceCount; i++) {
          buffer.append(' ');
        }
      }
      else {
        int size = totalSize;
        while (size > 0) {
          if (size >= indentOptions.TAB_SIZE) {
            buffer.append('\t');
            size -= indentOptions.TAB_SIZE;
          }
          else {
            buffer.append(' ');
            size--;
          }
        }
      }
    }
    else {
      for (int i = 0; i < totalSize; i++) {
        buffer.append(' ');
      }
    }

    return buffer.toString();
  }

  /**
   * @Depreacted Do not use the implementation, see {@link IndentHelper}
   */
  @Deprecated
  public static int getIndent(Project project, FileType fileType, String text, boolean includeNonSpace) {
    return getIndent(CodeStyle.getSettings(project).getIndentOptions(fileType), text, includeNonSpace);
  }

  public static int getIndent(@Nonnull PsiFile file, String text, boolean includeNonSpace) {
    return getIndent(CodeStyle.getIndentOptions(file), text, includeNonSpace);
  }

  public static int getIndent(@Nonnull CommonCodeStyleSettings.IndentOptions indentOptions, String text, boolean includeNonSpace) {
    int i;
    for (i = text.length() - 1; i >= 0; i--) {
      char c = text.charAt(i);
      if (c == '\n' || c == '\r') break;
    }
    i++;

    int spaceCount = 0;
    int tabCount = 0;
    for (int j = i; j < text.length(); j++) {
      char c = text.charAt(j);
      if (c != '\t') {
        if (!includeNonSpace && c != ' ') break;
        spaceCount++;
      }
      else {
        tabCount++;
      }
    }

    if (tabCount == 0) return spaceCount;

    int tabSize = indentOptions.TAB_SIZE;
    int indentSize = indentOptions.INDENT_SIZE;
    if (indentSize <= 0) {
      indentSize = 1;
    }
    int indentLevel = tabCount * tabSize / indentSize;
    return indentLevel * INDENT_FACTOR + spaceCount;
  }
}
