package com.intellij.semantic;

import consulo.logging.Logger;
import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.util.xmlb.annotations.Attribute;
import consulo.injecting.InjectingContainer;

/**
 * @author peter
 */
public class SemContributorEP extends AbstractExtensionPointBean {
  private static final Logger LOG = Logger.getInstance(SemContributorEP.class);

  @Attribute("implementation")
  public String implementation;

  public void registerSemProviders(InjectingContainer container, SemRegistrar registrar) {
    try {
      final SemContributor contributor = instantiate(implementation, container);
      contributor.registerSemProviders(registrar);
    }
    catch (ClassNotFoundException e) {
      LOG.error(e);
    }
  }
}
