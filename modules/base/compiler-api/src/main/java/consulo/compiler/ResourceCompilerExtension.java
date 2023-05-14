package consulo.compiler;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.module.Module;

import jakarta.annotation.Nonnull;

@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ResourceCompilerExtension {
  public abstract boolean skipStandardResourceCompiler(final @Nonnull Module module);
}
