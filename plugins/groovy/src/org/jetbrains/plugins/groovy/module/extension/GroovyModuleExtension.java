package org.jetbrains.plugins.groovy.module.extension;

import com.intellij.openapi.module.Module;
import org.consulo.module.extension.impl.ModuleExtensionImpl;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 14:47/28.05.13
 */
public class GroovyModuleExtension extends ModuleExtensionImpl<GroovyModuleExtension> {
  public GroovyModuleExtension(@NotNull String id, @NotNull Module module) {
    super(id, module);
  }
}
