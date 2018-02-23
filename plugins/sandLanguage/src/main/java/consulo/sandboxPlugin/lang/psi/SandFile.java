/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.lang.psi;

import com.intellij.extapi.psi.PsiFileBase;
import consulo.lang.LanguageVersion;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import javax.annotation.Nonnull;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.version.BaseSandLanguageVersion;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandFile extends PsiFileBase {
  public SandFile(@Nonnull FileViewProvider viewProvider) {
    super(viewProvider, SandLanguage.INSTANCE);
  }

  @Nonnull
  @Override
  public FileType getFileType() {
    LanguageVersion languageVersion = getLanguageVersion();
    if(languageVersion instanceof BaseSandLanguageVersion) {
      return ((BaseSandLanguageVersion)languageVersion).getFileType();
    }
    return SandFileType.INSTANCE;
  }
}
