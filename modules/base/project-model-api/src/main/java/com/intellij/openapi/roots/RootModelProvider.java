package com.intellij.openapi.roots;

import com.intellij.openapi.module.Module;
import javax.annotation.Nonnull;

/**
 * @author yole
 */
public interface RootModelProvider {
  @Nonnull
  Module[] getModules();

  ModuleRootModel getRootModel(@Nonnull Module module);
}
