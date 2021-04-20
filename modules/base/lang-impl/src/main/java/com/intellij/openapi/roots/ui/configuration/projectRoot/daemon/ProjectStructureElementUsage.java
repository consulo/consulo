package com.intellij.openapi.roots.ui.configuration.projectRoot.daemon;

import consulo.ui.image.Image;

import javax.annotation.Nullable;

/**
 * @author nik
 */
public abstract class ProjectStructureElementUsage {
  public abstract ProjectStructureElement getSourceElement();

  public abstract ProjectStructureElement getContainingElement();

  public abstract String getPresentableName();

  @Nullable
  public String getPresentableLocationInElement() {
    return null;
  }

  public abstract PlaceInProjectStructure getPlace();

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);

  public abstract Image getIcon();

  public abstract void removeSourceElement();
}
