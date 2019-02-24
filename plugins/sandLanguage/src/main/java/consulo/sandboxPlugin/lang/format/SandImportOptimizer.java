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

import com.intellij.lang.ImportOptimizer;
import com.intellij.psi.PsiFile;
import consulo.sandboxPlugin.lang.psi.SandFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-25
 */
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
}
