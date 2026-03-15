/*
 * Copyright 2013-2022 consulo.io
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
package consulo.sandboxPlugin.lang;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.copyright.BaseUpdateCopyrightsProvider;
import consulo.language.copyright.UpdatePsiFileCopyright;
import consulo.language.copyright.config.CopyrightFileConfig;
import consulo.language.copyright.config.CopyrightProfile;
import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.fileType.FileType;


/**
 * @author VISTALL
 * @since 28-Jun-22
 */
@ExtensionImpl
public class SandUpdateCopyrightsProvider extends BaseUpdateCopyrightsProvider {
  
  @Override
  public FileType getFileType() {
    return SandFileType.INSTANCE;
  }

  
  @Override
  public UpdatePsiFileCopyright<CopyrightFileConfig> createInstance(PsiFile file, CopyrightProfile copyrightProfile) {
    return new UpdatePsiFileCopyright<>(file, copyrightProfile) {
      @Override
      protected void scanFile() {

      }
    };
  }
}
