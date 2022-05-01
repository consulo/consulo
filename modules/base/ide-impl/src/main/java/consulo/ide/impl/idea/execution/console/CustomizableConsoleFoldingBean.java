package consulo.ide.impl.idea.execution.console;

import consulo.component.extension.AbstractExtensionPointBean;
import consulo.component.extension.ExtensionPointName;
import consulo.util.xml.serializer.annotation.Attribute;

/**
 * @author peter
 */
public class CustomizableConsoleFoldingBean extends AbstractExtensionPointBean {
  public static final ExtensionPointName<CustomizableConsoleFoldingBean> EP_NAME = ExtensionPointName.create("consulo.stacktrace.fold");

  @Attribute("substring")
  public String substring;

  @Attribute("negate")
  public boolean negate = false;

}
