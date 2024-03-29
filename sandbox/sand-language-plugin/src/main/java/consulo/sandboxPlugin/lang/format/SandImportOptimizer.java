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
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

  @Nonnull
  @Override
  public String getActionName() {
    return "Optimize 'using'";
  }

  @Nonnull
  @Override
  public Runnable processFile(PsiFile file) {
    return new CollectingInfoRunnable() {
      @Nullable
      @Override
      public String getUserNotificationInfo() {
        return "test";
      }

      @Override
      public void run() {

      }
    };
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return SandLanguage.INSTANCE;
  }
}
