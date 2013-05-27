package com.intellij.core;

import com.intellij.openapi.module.Module;
import org.consulo.module.extension.ModuleExtension;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 11:56/27.05.13
 */
public class CoreModuleExtension implements ModuleExtension<CoreModuleExtension> {
  @NotNull
  @Override
  public String getId() {
    return null;
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  @NotNull
  @Override
  public Module getModule() {
    return null;
  }

  @Override
  public void commit(@NotNull CoreModuleExtension mutableModuleExtension) {

  }

  @Nullable
  @Override
  public Element getState() {
    return null;
  }

  @Override
  public void loadState(Element state) {

  }
}
