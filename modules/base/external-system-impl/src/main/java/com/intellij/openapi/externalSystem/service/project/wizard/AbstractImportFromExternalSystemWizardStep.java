package com.intellij.openapi.externalSystem.service.project.wizard;

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import consulo.moduleImport.ModuleImportProvider;

import javax.annotation.Nonnull;

/**
 * Just a holder for the common useful functionality.
 * 
 * @author Denis Zhdanov
 * @since 8/2/11 3:22 PM
 */
public abstract class AbstractImportFromExternalSystemWizardStep extends ModuleWizardStep {

  @Nonnull
  private final WizardContext myContext;

  protected AbstractImportFromExternalSystemWizardStep(@Nonnull WizardContext context) {
    myContext = context;
  }

  public WizardContext getWizardContext() {
    return myContext;
  }

  public ModuleImportProvider getImportProvider() {
    throw new UnsupportedOperationException();
  }
}
