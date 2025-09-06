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

import consulo.application.Application;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes.ArtifactsTreeNode;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.compiler.artifact.ui.PackagingSourceItemsProvider;
import consulo.compiler.artifact.ui.TreeNodePresentation;
import consulo.ui.ex.awt.tree.SimpleNode;

import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author nik
 */
public abstract class SourceItemNodeBase extends ArtifactsTreeNode {
  private Artifact myArtifact;
  private final ArtifactEditorEx myArtifactEditor;

  public SourceItemNodeBase(ArtifactEditorContext context, NodeDescriptor parentDescriptor, TreeNodePresentation presentation,
                            ArtifactEditorEx artifactEditor) {
    super(context, parentDescriptor, presentation);
    myArtifact = artifactEditor.getArtifact();
    myArtifactEditor = artifactEditor;
  }

  protected ArtifactEditorEx getArtifactEditor() {
    return myArtifactEditor;
  }

  @Override
  protected void update(PresentationData presentation) {
    Artifact artifact = myArtifactEditor.getArtifact();
    if (!myArtifact.equals(artifact)) {
      myArtifact = artifact;
    }
    super.update(presentation);
  }

  @Override
  protected SimpleNode[] buildChildren() {
    List<SimpleNode> children = new ArrayList<>();
    Application.get().getExtensionPoint(PackagingSourceItemsProvider.class).forEachExtensionSafe(provider -> {
      Collection<? extends PackagingSourceItem> items = provider.getSourceItems(myContext, myArtifact, getSourceItem());
      for (PackagingSourceItem item : items) {
        if (myArtifact.getArtifactType().isSuitableItem(item)) {
          children.add(new SourceItemNode(myContext, this, item, myArtifactEditor));
        }
      }
    });
    return children.isEmpty() ? NO_CHILDREN : children.toArray(new SimpleNode[children.size()]);
  }

  @Nullable
  protected abstract PackagingSourceItem getSourceItem();
}
