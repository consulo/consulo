package consulo.compiler;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.module.Module;


@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ResourceCompilerExtension {
    public abstract boolean skipStandardResourceCompiler(Module module);
}
