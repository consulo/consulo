package consulo.language.sem;

import consulo.component.extension.AbstractExtensionPointBean;
import consulo.util.xml.serializer.annotation.Attribute;

/**
 * @author peter
 */
public class SemContributorEP extends AbstractExtensionPointBean {
  @Attribute("implementation")
  public String implementation;
}
