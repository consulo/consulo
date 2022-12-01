/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.language.editor.template.context;

import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TemplateContext {
  private final Map<String, Boolean> myContextStates = new LinkedHashMap<>();

  public TemplateContext createCopy() {
    TemplateContext cloneResult = new TemplateContext();
    cloneResult.myContextStates.putAll(myContextStates);
    return cloneResult;
  }

  public Map<TemplateContextType, Boolean> getDifference(@Nullable TemplateContext defaultContext) {
    Map<TemplateContextType, Boolean> result = new LinkedHashMap<>();
    synchronized (myContextStates) {
      //noinspection NestedSynchronizedStatement
      synchronized (defaultContext == null ? myContextStates : defaultContext.myContextStates) {
        for (TemplateContextType contextType : TemplateContextType.EP_NAME.getExtensions()) {
          String context = contextType.getContextId();
          Boolean myStateInContext = myContextStates.get(context);
          if (myStateInContext != null && differsFromDefault(defaultContext, context, myStateInContext)) {
            result.put(contextType, myStateInContext);
          }
        }
      }
    }
    return result;
  }

  private static boolean differsFromDefault(@Nullable TemplateContext defaultContext, String context, boolean myStateInContext) {
    Boolean defaultStateInContext = defaultContext == null ? null : defaultContext.myContextStates.get(context);
    if (defaultStateInContext == null) {
      return true;
    }
    return myStateInContext != defaultStateInContext;
  }

  public boolean isEnabled(TemplateContextType contextType) {
    Boolean storedValue = isEnabledBare(contextType);
    if (storedValue == null) {
      TemplateContextType baseContextType = contextType.getBaseContextType();
      if (baseContextType != null && !(baseContextType instanceof EverywhereContextType)) {
        return isEnabled(baseContextType);
      }
      return false;
    }
    return storedValue.booleanValue();
  }

  private Boolean isEnabledBare(TemplateContextType contextType) {
    synchronized (myContextStates) {
      return myContextStates.get(contextType.getContextId());
    }
  }

  public void setEnabled(TemplateContextType contextType, boolean value) {
    synchronized (myContextStates) {
      myContextStates.put(contextType.getContextId(), value);
    }
  }

  public void setDefaultContext(@Nonnull TemplateContext defContext) {
    HashMap<String, Boolean> copy = new HashMap<String, Boolean>(myContextStates);
    myContextStates.clear();
    myContextStates.putAll(defContext.myContextStates);
    myContextStates.putAll(copy);
  }

  public void readTemplateContext(Element element) throws InvalidDataException {
    List options = element.getChildren("option");
    for (Object e : options) {
      if (e instanceof Element) {
        Element option = (Element)e;
        String name = option.getAttributeValue("name");
        String value = option.getAttributeValue("value");
        if (name != null && value != null) {
          myContextStates.put(name, Boolean.parseBoolean(value));
        }
      }
    }
  }

  public void writeTemplateContext(Element element, @Nullable TemplateContext defaultContext) throws WriteExternalException {
    Map<TemplateContextType, Boolean> diff = getDifference(defaultContext);
    for (TemplateContextType type : diff.keySet()) {
      Element optionElement = new Element("option");
      optionElement.setAttribute("name", type.getContextId());
      optionElement.setAttribute("value", diff.get(type).toString());
      element.addContent(optionElement);
    }
  }
}
