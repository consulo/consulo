package com.intellij.openapi.externalSystem.model.project;

import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 8/12/11 12:34 PM
 */
public interface Named {

  /**
   * please use {@link #getExternalName()} or {@link #getInternalName()} instead
   */
  @Nonnull
  @Deprecated
  String getName();

  /**
   * please use {@link #setExternalName(String)} or {@link #setInternalName(String)} instead
   */
  @Deprecated
  void setName(@Nonnull String name);

  @Nonnull
  String getExternalName();
  void setExternalName(@Nonnull String name);

  @Nonnull
  String getInternalName();
  void setInternalName(@Nonnull String name);
}
