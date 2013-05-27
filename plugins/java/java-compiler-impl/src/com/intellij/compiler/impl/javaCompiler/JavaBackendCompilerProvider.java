package com.intellij.compiler.impl.javaCompiler;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.consulo.compiler.CompilerSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 10:34/27.05.13
 */
public interface JavaBackendCompilerProvider {
  ExtensionPointName<JavaBackendCompilerProvider> EP_NAME = ExtensionPointName.create("org.consulo.java.compilerProvider");

  @NotNull
  BackendCompiler createCompiler(@NotNull Project project);

  @Nullable
  CompilerSettings createSettings(@NotNull Project project);
}
