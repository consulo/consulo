package org.jetbrains.plugins.groovy.module.extension;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableRootModel;
import lombok.NonNull;
import org.consulo.module.extension.MutableModuleExtensionWithSdk;
import org.consulo.module.extension.MutableModuleInheritableNamedPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 14:47/28.05.13
 */
public class GroovyMutableModuleExtension extends GroovyModuleExtension implements MutableModuleExtensionWithSdk<GroovyModuleExtension> {
  @NotNull
  private final GroovyModuleExtension myModuleExtension;

  public GroovyMutableModuleExtension(@NotNull String id, @NotNull Module module, @NotNull GroovyModuleExtension moduleExtension) {
    super(id, module);
    myModuleExtension = moduleExtension;
  }

  @NotNull
  @Override
  public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk() {
    return (MutableModuleInheritableNamedPointer<Sdk>)super.getInheritableSdk();
  }

  @Nullable
  @Override
  public JComponent createConfigurablePanel(@NonNull ModifiableRootModel rootModel, @Nullable Runnable updateOnCheck) {
    return null;
  }

  @Override
  public void setEnabled(boolean val) {
    myIsEnabled = val;
  }

  @Override
  public boolean isModified() {
    return isModifiedImpl(myModuleExtension);
  }

  @Override
  public void commit() {
    myModuleExtension.commit(this);
  }
}
