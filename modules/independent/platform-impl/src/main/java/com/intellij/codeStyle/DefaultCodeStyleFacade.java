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

/*
 * @author max
 */
package com.intellij.codeStyle;

import com.intellij.lang.Language;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.options.Configurable;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DefaultCodeStyleFacade extends CodeStyleFacade {
  @Override
  public int getIndentSize(final FileType fileType) {
    return 4;
  }

  @Override
  @Nullable
  public String getLineIndent(@Nonnull final Document document, int offset) {
    return null;
  }

  @Override
  public String getLineSeparator() {
    return "\n";
  }

  public int getRightMargin(Language language) {
    return 80;
  }

  @Override
  public boolean isWrapWhenTypingReachesRightMargin() {
    return false;
  }

  @Override
  public int getTabSize(final FileType fileType) {
    return 4;
  }

  @Override
  public boolean isSmartTabs(final FileType fileType) {
    return false;
  }

  @Override
  public boolean projectUsesOwnSettings() {
    return false;
  }

  @Override
  public boolean isUnsuitableCodeStyleConfigurable(final Configurable c) {
    return false;
  }

  @Override
  public boolean useTabCharacter(final FileType fileType) {
    return false;
  }
}