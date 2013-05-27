package com.intellij.packaging.impl.artifacts;

import com.intellij.openapi.project.Project;
import com.intellij.packaging.impl.compiler.ArtifactsCompiler;
import org.consulo.compiler.CompilerProvider;
import org.consulo.compiler.CompilerSettings;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 10:17/27.05.13
 */
public class ArtifactsCompilerProvider implements CompilerProvider<ArtifactsCompiler> {
  @NotNull
  @Override
  public ArtifactsCompiler createCompiler(Project project) {
    return new ArtifactsCompiler();
  }

  @Override
  public CompilerSettings createSettings(@NotNull Project project) {
    return null;
  }
}
