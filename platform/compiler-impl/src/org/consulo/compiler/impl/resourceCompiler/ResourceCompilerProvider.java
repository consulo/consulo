package org.consulo.compiler.impl.resourceCompiler;

import org.consulo.compiler.CompilerProvider;
import org.consulo.compiler.CompilerSettings;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 20:16/24.05.13
 */
public class ResourceCompilerProvider implements CompilerProvider<ResourceCompiler> {
  @NotNull
  @Override
  public ResourceCompiler createCompiler() {
    return new ResourceCompiler();
  }

  @Override
  public CompilerSettings<ResourceCompiler> createSettings() {
    return new ResourceCompilerSettings();
  }
}
