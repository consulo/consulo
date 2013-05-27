package com.intellij.compiler.impl.javaCompiler.javac;

import com.intellij.compiler.impl.javaCompiler.BackendCompiler;
import com.intellij.compiler.impl.javaCompiler.JavaBackendCompilerProvider;
import com.intellij.openapi.project.Project;
import org.consulo.compiler.CompilerSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 10:36/27.05.13
 */
public class JavacBackendCompilerProvider implements JavaBackendCompilerProvider {
  @NotNull
  @Override
  public BackendCompiler createCompiler(@NotNull Project project) {
    return new JavacCompiler(project);
  }

  @Nullable
  @Override
  public CompilerSettings createSettings(@NotNull Project project) {
    return new JavacCompilerSettings(project);
  }
}
