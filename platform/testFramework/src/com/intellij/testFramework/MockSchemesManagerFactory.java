package com.intellij.testFramework;

import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.options.*;
import org.consulo.util.pointers.Named;

public class MockSchemesManagerFactory extends SchemesManagerFactory {
  @Override
  public <T extends Named,E extends ExternalizableScheme> SchemesManager<T,E> createSchemesManager(final String fileSpec,
                                                                   final SchemeProcessor<E> processor, final RoamingType roamingType) {
    return SchemesManager.EMPTY;
  }

  @Override
  public void updateConfigFilesFromStreamProviders() {
    
  }
}
