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
package consulo.sandboxPlugin.lang.format;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.refactoring.ImportOptimizer;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandFile;

/**
 * @author VISTALL
 * @since 2019-02-25
 */
@ExtensionImpl
public class SandImportOptimizer implements ImportOptimizer {
  @Override
  public boolean supports(PsiFile file) {
    return file instanceof SandFile;
  }

  @Override
  public LocalizeValue getActionName() {
    return LocalizeValue.of("Optimize 'using'");
  }

  @Override
  public Runnable processFile(PsiFile file) {
    return new CollectingInfoRunnable() {
      @Override
      public LocalizeValue getUserNotificationInfo() {
        return LocalizeValue.of("test");
      }

      @Override
      public void run() {
      }
    };
  }

  @Override
  public Language getLanguage() {
    return SandLanguage.INSTANCE;
  }
}
