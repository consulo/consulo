package com.intellij.openapi.roots;

import consulo.module.Module;
import consulo.module.layer.ModuleRootModel;

import javax.annotation.Nonnull;

/**
 * @author yole
 */
public interface RootModelProvider {
  @Nonnull
  Module[] getModules();

  ModuleRootModel getRootModel(@Nonnull Module module);
}
