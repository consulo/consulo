package org.consulo.compiler;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 19:20/24.05.13
 */
public interface CompilerProvider<T extends com.intellij.openapi.compiler.Compiler> {
  ExtensionPointName<CompilerProvider> EP_NAME = ExtensionPointName.create("com.intellij.compilerProvider");

  @NotNull
  T createCompiler();

  CompilerSettings<T> createSettings();
}
