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
package consulo.language.editor.template.macro;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.template.TextResult;
import consulo.localize.LocalizeValue;
import consulo.virtualFileSystem.VirtualFile;

/**
 * @author Nikolay Matveev
 */
@ExtensionImpl
public class FileNameWithoutExtensionMacro extends FileNameMacro {

  @Override
  public String getName() {
    return "fileNameWithoutExtension";
  }

  @Override
  public LocalizeValue getPresentableName() {
    return CodeInsightLocalize.macroFileNameWithoutExtension();
  }

  @Override
  protected TextResult calculateResult(VirtualFile virtualFile) {
    return new TextResult(virtualFile.getNameWithoutExtension());
  }
}
