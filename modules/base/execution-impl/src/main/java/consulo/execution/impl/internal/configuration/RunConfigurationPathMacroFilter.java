/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.impl.internal.configuration;

import consulo.annotation.component.ExtensionImpl;
import consulo.component.macro.PathMacroFilter;
import consulo.util.xml.serializer.Constants;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * @author yole
 */
@ExtensionImpl
public class RunConfigurationPathMacroFilter extends PathMacroFilter {
  @Override
  public boolean skipPathMacros(Attribute attribute) {
    return attribute.getName().equals(Constants.NAME) && attribute.getParent().getName().equals("configuration");
  }

  @Override
  public boolean recursePathMacros(Attribute attribute) {
    final Element parent = attribute.getParent();
    if (parent != null && Constants.OPTION.equals(parent.getName())) {
      final Element grandParent = parent.getParentElement();
      return grandParent != null && "configuration".equals(grandParent.getName());
    }
    return false;
  }
}
