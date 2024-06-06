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

import consulo.application.AllIcons;
import consulo.ui.ex.tree.PresentationData;
import consulo.util.lang.Comparing;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingSourceItem;
import consulo.compiler.artifact.ui.SourceItemPresentation;
import consulo.ide.impl.idea.packaging.ui.SourceItemWeights;
import consulo.ui.ex.SimpleTextAttributes;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public class ModuleGroupItem extends PackagingSourceItem {
  private final String myGroupName;
  private final String[] myPath;

  public ModuleGroupItem(String[] path) {
    super(false);
    myGroupName = path[path.length - 1];
    myPath = path;
  }

  public boolean equals(Object obj) {
    return obj instanceof ModuleGroupItem && Comparing.equal(myPath, ((ModuleGroupItem)obj).myPath);
  }

  public int hashCode() {
    return Arrays.hashCode(myPath);
  }

  @Override
  public SourceItemPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    return new ModuleGroupSourceItemPresentation(myGroupName);
  }

  @Nonnull
  @Override
  public List<? extends PackagingElement<?>> createElements(@Nonnull ArtifactEditorContext context) {
    return Collections.emptyList();
  }

  public String[] getPath() {
    return myPath;
  }

  private static class ModuleGroupSourceItemPresentation extends SourceItemPresentation {
    private final String myGroupName;

    public ModuleGroupSourceItemPresentation(String groupName) {
      myGroupName = groupName;
    }

    @Override
    public String getPresentableName() {
      return myGroupName;
    }

    @Override
    public void render(@Nonnull PresentationData presentationData, SimpleTextAttributes mainAttributes,
                       SimpleTextAttributes commentAttributes) {
      presentationData.setIcon(AllIcons.Nodes.ModuleGroup);
      presentationData.addText(myGroupName, mainAttributes);
    }

    @Override
    public int getWeight() {
      return SourceItemWeights.MODULE_GROUP_WEIGHT;
    }
  }
}
