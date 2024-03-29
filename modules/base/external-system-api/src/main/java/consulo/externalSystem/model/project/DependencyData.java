package consulo.externalSystem.model.project;

import consulo.externalSystem.service.project.ExternalEntityData;
import consulo.module.content.layer.orderEntry.DependencyScope;

import jakarta.annotation.Nonnull;

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
