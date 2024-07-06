/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff;

import consulo.desktop.awt.internal.diff.util.AWTDiffUtil;
import consulo.diff.DiffDialogHints;
import consulo.diff.chain.DiffRequestChain;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class DiffWindow extends DiffWindowBase {
  @Nonnull
  private final DiffRequestChain myRequestChain;

  public DiffWindow(@Nullable Project project, @Nonnull DiffRequestChain requestChain, @Nonnull DiffDialogHints hints) {
    super(project, hints);
    myRequestChain = requestChain;
  }

  @Nonnull
  @Override
  protected DiffRequestProcessor createProcessor() {
    return new MyCacheDiffRequestChainProcessor(myProject, myRequestChain);
  }

  private class MyCacheDiffRequestChainProcessor extends CacheDiffRequestChainProcessor {
    public MyCacheDiffRequestChainProcessor(@Nullable Project project, @Nonnull DiffRequestChain requestChain) {
      super(project, requestChain);
    }

    @Override
    protected void setWindowTitle(@Nonnull String title) {
      getWrapper().setTitle(title);
    }

    @Override
    protected void onAfterNavigate() {
      AWTDiffUtil.closeWindow(getWrapper().getWindow(), true, true);
    }
  }
}
