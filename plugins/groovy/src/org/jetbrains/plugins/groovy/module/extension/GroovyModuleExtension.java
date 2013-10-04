package org.jetbrains.plugins.groovy.module.extension;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.SdkType;
import org.consulo.module.extension.ModuleExtensionWithSdk;
import org.consulo.module.extension.impl.ModuleExtensionWithSdkImpl;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 14:47/28.05.13
 */
public class GroovyModuleExtension extends ModuleExtensionWithSdkImpl<GroovyModuleExtension>
  implements ModuleExtensionWithSdk<GroovyModuleExtension> {
  public GroovyModuleExtension(@NotNull String id, @NotNull Module module) {
    super(id, module);
  }

  @Override
  protected Class<? extends SdkType> getSdkTypeClass() {
    return JavaSdk.class;
  }
}
