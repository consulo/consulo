package consulo.ide.impl.idea.compiler.impl.resourceCompiler;

import consulo.component.extension.ExtensionPointName;
import consulo.module.Module;
import javax.annotation.Nonnull;

public abstract class ResourceCompilerExtension {
  public static final ExtensionPointName<ResourceCompilerExtension> EP_NAME =
    ExtensionPointName.create("consulo.compiler.resourceCompilerExtension");

  public boolean skipStandardResourceCompiler(final @Nonnull Module module) {
    return false;
  }
}
