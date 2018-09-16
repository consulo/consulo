package com.intellij.openapi.externalSystem.model.project;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 8/25/11 3:44 PM
 */
public abstract class AbstractExternalEntityData implements ExternalEntityData {

  private static final long serialVersionUID = 1L;
  
  @Nonnull
  private ProjectSystemId myOwner;
  
  public AbstractExternalEntityData(@Nonnull ProjectSystemId owner) {
    myOwner = owner;
  }

  @Override
  @Nonnull
  public ProjectSystemId getOwner() {
    return myOwner;
  }

  @Override
  public int hashCode() {
    return myOwner.hashCode();
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    AbstractExternalEntityData that = (AbstractExternalEntityData)obj;
    return myOwner.equals(that.myOwner);
  }
} 