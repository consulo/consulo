package com.intellij.openapi.externalSystem.model.project;

import javax.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 8/10/11 6:40 PM
 */
public class ModuleDependencyData extends AbstractDependencyData<ModuleData> {

  private static final long serialVersionUID = 1L;

  public ModuleDependencyData(@Nonnull ModuleData ownerModule, @Nonnull ModuleData module) {
    super(ownerModule, module);
  }
}
