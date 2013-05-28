package org.jetbrains.plugins.groovy.module.extension;

import com.intellij.openapi.module.Module;
import org.consulo.module.extension.MutableModuleExtension;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 14:47/28.05.13
 */
public class GroovyMutableModuleExtension extends GroovyModuleExtension implements MutableModuleExtension<GroovyModuleExtension> {
  @NotNull
  private final GroovyModuleExtension myModuleExtension;

  public GroovyMutableModuleExtension(@NotNull String id, @NotNull Module module, @NotNull GroovyModuleExtension moduleExtension) {
    super(id, module);
    myModuleExtension = moduleExtension;
    commit(myModuleExtension);
  }

  @Nullable
  @Override
  public JComponent createConfigurablePanel(@Nullable Runnable updateOnCheck) {
    return null;
  }

  @Override
  public void setEnabled(boolean val) {
    myIsEnabled = val;
  }

  @Override
  public boolean isModified() {
    return myIsEnabled != myModuleExtension.isEnabled();
  }

  @Override
  public void commit() {
    myModuleExtension.commit(this);
  }
}
