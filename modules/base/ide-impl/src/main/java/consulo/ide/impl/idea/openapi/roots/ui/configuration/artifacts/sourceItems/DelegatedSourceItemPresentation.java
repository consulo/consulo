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

import consulo.compiler.artifact.ui.SourceItemPresentation;
import consulo.compiler.artifact.ui.TreeNodePresentation;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.ex.SimpleTextAttributes;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class DelegatedSourceItemPresentation extends SourceItemPresentation {
  private final TreeNodePresentation myPresentation;

  public DelegatedSourceItemPresentation(TreeNodePresentation presentation) {
    myPresentation = presentation;
  }

  @Override
  public String getPresentableName() {
    return myPresentation.getPresentableName();
  }

  @Override
  public String getSearchName() {
    return myPresentation.getSearchName();
  }

  @Override
  public void render(@Nonnull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    myPresentation.render(presentationData, mainAttributes, commentAttributes);
  }

  @Override
  @Nullable
  public String getTooltipText() {
    return myPresentation.getTooltipText();
  }

  @Override
  public boolean canNavigateToSource() {
    return myPresentation.canNavigateToSource();
  }

  @Override
  public void navigateToSource() {
    myPresentation.navigateToSource();
  }

  @Override
  public int getWeight() {
    return myPresentation.getWeight();
  }
}
