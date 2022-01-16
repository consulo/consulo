package com.intellij.compiler.impl.resourceCompiler;

import consulo.component.extension.ExtensionPointName;
import com.intellij.openapi.module.Module;
import javax.annotation.Nonnull;

public abstract class ResourceCompilerExtension {
  public static final ExtensionPointName<ResourceCompilerExtension> EP_NAME =
    ExtensionPointName.create("consulo.base.compiler.resourceCompilerExtension");

  public boolean skipStandardResourceCompiler(final @Nonnull Module module) {
    return false;
  }
}
