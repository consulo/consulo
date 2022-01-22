package consulo.module.content.layer;

import consulo.module.Module;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public interface RootModelProvider {
  @Nonnull
  Module[] getModules();

  ModuleRootModel getRootModel(@Nonnull Module module);
}
