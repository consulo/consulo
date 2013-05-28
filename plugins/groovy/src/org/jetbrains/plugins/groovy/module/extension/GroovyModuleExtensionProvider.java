package org.jetbrains.plugins.groovy.module.extension;

import com.intellij.openapi.module.Module;
import icons.JetgroovyIcons;
import org.consulo.module.extension.ModuleExtensionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 14:49/28.05.13
 */
public class GroovyModuleExtensionProvider implements ModuleExtensionProvider<GroovyModuleExtension, GroovyMutableModuleExtension> {
  @Nullable
  @Override
  public Icon getIcon() {
    return JetgroovyIcons.Groovy.Groovy_16x16;
  }

  @NotNull
  @Override
  public String getName() {
    return "Groovy";
  }

  @NotNull
  @Override
  public Class<GroovyModuleExtension> getImmutableClass() {
    return GroovyModuleExtension.class;
  }

  @NotNull
  @Override
  public GroovyModuleExtension createImmutable(@NotNull String id, @NotNull Module module) {
    return new GroovyModuleExtension(id, module);
  }

  @NotNull
  @Override
  public GroovyMutableModuleExtension createMutable(@NotNull String id,
                                                    @NotNull Module module,
                                                    @NotNull GroovyModuleExtension moduleExtension) {
    return new GroovyMutableModuleExtension(id, module, moduleExtension);
  }
}
