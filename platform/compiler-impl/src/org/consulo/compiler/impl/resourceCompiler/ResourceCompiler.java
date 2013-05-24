package org.consulo.compiler.impl.resourceCompiler;

import com.intellij.openapi.compiler.*;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 20:15/24.05.13
 */
public class ResourceCompiler implements com.intellij.openapi.compiler.Compiler {
  @NotNull
  @Override
  public String getDescription() {
    return "ResourceCompiler";
  }

  @Override
  public boolean validateConfiguration(CompileScope scope) {
    return false;
  }
}
