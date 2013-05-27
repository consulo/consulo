package com.intellij.compiler.impl.javaCompiler;

import com.intellij.openapi.project.Project;
import org.consulo.compiler.CompilerProvider;
import org.consulo.compiler.CompilerSettings;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 16:26/26.05.13
 */
public class JavaCompilerProvider implements CompilerProvider<JavaCompiler> {
  @NotNull
  @Override
  public JavaCompiler createCompiler(Project project) {
    return new JavaCompiler(project);
  }

  @Override
  public CompilerSettings createSettings(@NotNull Project project) {
    return new JavaCompilerSettings(project);
  }
}
