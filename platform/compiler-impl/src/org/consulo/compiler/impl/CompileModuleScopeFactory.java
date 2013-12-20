package org.consulo.compiler.impl;

import com.intellij.compiler.impl.FileIndexCompileScope;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12:55/20.12.13
 */
public interface CompileModuleScopeFactory {
  ExtensionPointName<CompileModuleScopeFactory> EP_NAME = ExtensionPointName.create("com.intellij.compiler.moduleScopeFactory");

  @Nullable
  FileIndexCompileScope createScope(@NotNull final Module module, final boolean includeDependentModules);
}
