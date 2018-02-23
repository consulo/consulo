package com.intellij.openapi.externalSystem.model.project;

import com.intellij.openapi.roots.DependencyScope;
import javax.annotation.Nonnull;

/**
 * Implementations of this interface are not obliged to be thread-safe.
 * 
 * @author Denis Zhdanov
 * @since 8/10/11 6:31 PM
 */
public interface DependencyData<T extends ExternalEntityData> extends ExternalEntityData {
  
  boolean isExported();

  @Nonnull
  DependencyScope getScope();

  @Nonnull
  ModuleData getOwnerModule();
  
  @Nonnull
  T getTarget();
}
