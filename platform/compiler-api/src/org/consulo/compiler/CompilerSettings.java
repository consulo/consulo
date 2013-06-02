package org.consulo.compiler;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 19:58/24.05.13
 */
public interface CompilerSettings{
  CompilerSettings EMPTY = new CompilerSettings() {

    @Override
    public Configurable createConfigurable() {
      return null;
    }
  };
  @Nullable
  Configurable createConfigurable();
}
