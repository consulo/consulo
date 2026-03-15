package consulo.module.content.layer;

import consulo.module.Module;


/**
 * @author yole
 */
public interface RootModelProvider {
  
  Module[] getModules();

  ModuleRootModel getRootModel(Module module);
}
