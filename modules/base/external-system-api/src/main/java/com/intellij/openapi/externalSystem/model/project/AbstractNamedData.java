package com.intellij.openapi.externalSystem.model.project;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 8/25/11 5:38 PM
 */
public abstract class AbstractNamedData extends AbstractExternalEntityData implements Named {

  private static final long serialVersionUID = 1L;

  @Nonnull
  private String myExternalName;
  @Nonnull
  private String myInternalName;

  public AbstractNamedData(@Nonnull ProjectSystemId owner, @Nonnull String externalName) {
    this(owner, externalName, externalName);
  }

  public AbstractNamedData(@Nonnull ProjectSystemId owner, @Nonnull String externalName, @Nonnull String internalName) {
    super(owner);
    myExternalName = externalName;
    myInternalName = internalName;
  }

  /**
   * please use {@link #getExternalName()} or {@link #getInternalName()} instead
   */
  @Nonnull
  @Deprecated
  @Override
  public String getName() {
    return getExternalName();
  }

  /**
   * please use {@link #setExternalName(String)} or {@link #setInternalName(String)} instead
   */
  @Deprecated
  @Override
  public void setName(@Nonnull String name) {
    setExternalName(name);
  }

  @Nonnull
  @Override
  public String getExternalName() {
    return myExternalName;
  }

  @Override
  public void setExternalName(@Nonnull String name) {
    myExternalName = name;
  }

  @Nonnull
  @Override
  public String getInternalName() {
    return myInternalName;
  }

  @Override
  public void setInternalName(@Nonnull String name) {
    myInternalName = name;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + myExternalName.hashCode();
    result = 31 * result + myInternalName.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;

    AbstractNamedData data = (AbstractNamedData)o;

    if (!myExternalName.equals(data.myExternalName)) return false;
    if (!myInternalName.equals(data.myInternalName)) return false;
    return true;
  }
}
