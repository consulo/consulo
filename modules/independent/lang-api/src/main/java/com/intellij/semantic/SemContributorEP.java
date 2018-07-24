package com.intellij.semantic;

import com.google.inject.Injector;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.util.xmlb.annotations.Attribute;

/**
 * @author peter
 */
public class SemContributorEP extends AbstractExtensionPointBean {
  private static final Logger LOG = Logger.getInstance("#com.intellij.semantic.SemContributorEP");

  @Attribute("implementation")
  public String implementation;

  public void registerSemProviders(Injector container, SemRegistrar registrar) {
    try {
      final SemContributor contributor = instantiate(implementation, container);
      contributor.registerSemProviders(registrar);
    }
    catch (ClassNotFoundException e) {
      LOG.error(e);
    }
  }

}
