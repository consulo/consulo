package consulo.module.extension;

import consulo.annotations.Immutable;
import consulo.lombok.annotations.ProjectService;
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
