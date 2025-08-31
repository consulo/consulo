/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.packaging.impl.run;

import consulo.application.AllIcons;
import consulo.ui.ex.awt.ElementsChooser;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactPointer;
import consulo.ui.ex.JBColor;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import java.awt.*;
import java.util.Comparator;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactChooser extends ElementsChooser<ArtifactPointer> {
  private static final Comparator<ArtifactPointer> ARTIFACT_COMPARATOR = (o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName());
  private static final ElementProperties INVALID_ARTIFACT_PROPERTIES = new ElementProperties() {
    @Override
    public Image getIcon() {
      return AllIcons.Nodes.Artifact;
    }

    @Override
    public Color getColor() {
      return JBColor.RED;
    }
  };

  public ArtifactChooser(List<ArtifactPointer> pointers) {
    super(pointers, false);
    for (ArtifactPointer pointer : pointers) {
      if (pointer.get() == null) {
        setElementProperties(pointer, INVALID_ARTIFACT_PROPERTIES);
      }
    }
    sort(ARTIFACT_COMPARATOR);
  }

  @Override
  protected String getItemText(@Nonnull ArtifactPointer value) {
    return value.getName();
  }

  @Override
  protected Image getItemIcon(@Nonnull ArtifactPointer value) {
    Artifact artifact = value.get();
    return artifact != null ? artifact.getArtifactType().getIcon() : null;
  }
}
