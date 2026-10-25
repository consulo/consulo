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
package consulo.diff.impl.internal.editor;

import consulo.diff.DiffEditorViewer;
import consulo.diff.DiffViewerVirtualFile;
import consulo.diff.chain.DiffRequestChain;
import consulo.diff.combined.CombinedBlockProducer;
import consulo.diff.combined.CombinedDiffComponentProcessor;
import consulo.diff.combined.CombinedDiffKeys;
import consulo.diff.combined.CombinedDiffManager;
import consulo.diff.combined.CombinedDiffRequestChain;
import consulo.diff.combined.CombinedDiffToggle;
import consulo.diff.impl.internal.CacheDiffRequestChainProcessor;
import consulo.diff.impl.internal.DiffRequestProcessor;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import java.util.List;

// from kotlin
public class ChainDiffVirtualFile extends DiffViewerVirtualFile {
  
  private final DiffRequestChain myChain;

  public ChainDiffVirtualFile(DiffRequestChain chain, String name) {
    super(name);
    myChain = chain;
  }

  @Override
  @RequiredUIAccess
  public DiffEditorViewer createViewer(Project project) {
    if (myChain instanceof CombinedDiffRequestChain combinedChain) {
      DiffEditorViewer viewer;
      List<CombinedBlockProducer> blocks = combinedChain.getCombinedBlocks();
      if (CombinedDiffToggle.DEFAULT.isCombinedDiffEnabled() && blocks.size() > 1) {
        CombinedDiffComponentProcessor processor = CombinedDiffManager.getInstance(project).createProcessor();
        int index = combinedChain.getIndex();
        if (index >= 0 && index < blocks.size()) {
          processor.getContext().putUserData(CombinedDiffKeys.COMBINED_DIFF_SCROLL_TO_BLOCK, blocks.get(index).getId());
        }
        processor.setBlocks(blocks);
        viewer = processor;
      }
      else {
        viewer = new CacheDiffRequestChainProcessor(project, myChain);
      }
      viewer.getContext().putUserData(CombinedDiffKeys.COMBINED_DIFF_TOGGLE, CombinedDiffToggle.DEFAULT);
      return viewer;
    }
    return new CacheDiffRequestChainProcessor(project, myChain);
  }

  @Override
  public String toString() {
    return "ChainDiffVirtualFile{" + "myChain=" + myChain + '}';
  }
}
