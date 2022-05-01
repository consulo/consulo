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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes;

import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.TreeNodePresentation;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.tree.CachingSimpleNode;

/**
 * @author nik
 */
public abstract class ArtifactsTreeNode extends CachingSimpleNode {
  private final TreeNodePresentation myPresentation;
  protected final ArtifactEditorContext myContext;

  protected ArtifactsTreeNode(ArtifactEditorContext context, NodeDescriptor parentDescriptor, final TreeNodePresentation presentation) {
    super(context.getProject(), parentDescriptor);
    myContext = context;
    myPresentation = presentation;
  }

  @Override
  protected void update(PresentationData presentation) {
    myPresentation.render(presentation, SimpleTextAttributes.REGULAR_ATTRIBUTES, SimpleTextAttributes.GRAY_ATTRIBUTES);
    presentation.setTooltip(myPresentation.getTooltipText());
  }

  public TreeNodePresentation getElementPresentation() {
    return myPresentation;
  }

  @Override
  public int getWeight() {
    return myPresentation.getWeight();
  }

  @Override
  public String getName() {
    return myPresentation.getPresentableName();
  }
}
