package org.jetbrains.plugins.groovy.griffon.module.extension;

import com.intellij.openapi.module.Module;
import org.consulo.module.extension.impl.ModuleExtensionImpl;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 14:33/30.06.13
 */
public class GriffonModuleExtension extends ModuleExtensionImpl<GriffonModuleExtension> {
  public GriffonModuleExtension(@NotNull String id, @NotNull Module module) {
    super(id, module);
  }
}
