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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.sourceItems;

import consulo.ui.ex.tree.PresentationData;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorImpl;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.compiler.artifact.ui.TreeNodePresentation;
import consulo.ui.ex.SimpleTextAttributes;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class SourceItemsTreeRoot extends SourceItemNodeBase {
  public SourceItemsTreeRoot(ArtifactEditorContext context, ArtifactEditorImpl artifactsEditor) {
    super(context, null, new RootNodePresentation(), artifactsEditor);
  }

  @Override
  protected PackagingSourceItem getSourceItem() {
    return null;
  }

  @Nonnull
  @Override
  public Object[] getEqualityObjects() {
    return new Object[]{"root"};
  }

  @Override
  public boolean isAutoExpandNode() {
    return true;
  }

  private static class RootNodePresentation extends TreeNodePresentation {
    @Override
    public String getPresentableName() {
      return "";
    }

    @Override
    public void render(@Nonnull PresentationData presentationData, SimpleTextAttributes mainAttributes,
                       SimpleTextAttributes commentAttributes) {
    }

    @Override
    public int getWeight() {
      return 0;
    }
  }
}
