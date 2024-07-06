/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.awt.internal.diff.editor;

import consulo.desktop.awt.internal.diff.DiffRequestProcessor;
import consulo.diff.chain.DiffRequestChain;
import consulo.diff.impl.internal.editor.DiffVirtualFile;
import consulo.desktop.awt.internal.diff.CacheDiffRequestChainProcessor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

// from kotlin
public class ChainDiffVirtualFile extends DiffVirtualFile {
  @Nonnull
  private final DiffRequestChain myChain;

  public ChainDiffVirtualFile(@Nonnull DiffRequestChain chain, @Nonnull String name) {
    super(name);
    myChain = chain;
  }

  @Override
  public DiffRequestProcessor createProcessor(Project project) {
    return new CacheDiffRequestChainProcessor(project, myChain);
  }

  @Override
  public String toString() {
    return "ChainDiffVirtualFile{" + "myChain=" + myChain + '}';
  }
}
