package org.consulo.compiler;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 19:20/24.05.13
 */
public interface CompilerProvider<T extends com.intellij.openapi.compiler.Compiler> {
  ExtensionPointName<CompilerProvider> EP_NAME = ExtensionPointName.create("com.intellij.compilerProvider");

  @NotNull
  T createCompiler(Project project);

  @Nullable
  CompilerSettings createSettings(@NotNull Project project);
}
