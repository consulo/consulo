package org.mustbe.consulo.module.extension;

import org.consulo.annotations.Immutable;
import org.consulo.lombok.annotations.ProjectService;
import org.consulo.module.extension.ModuleExtension;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 7:57/12.11.13
 */
@ProjectService
public abstract class ModuleExtensionHelper {
  public abstract boolean hasModuleExtension(@NotNull Class<? extends ModuleExtension> clazz);

  @Immutable
  @NotNull
  public abstract <T extends ModuleExtension<T>> Collection<T> getModuleExtensions(@NotNull Class<T> clazz);
}
