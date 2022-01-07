/*
 * Copyright 2013-2017 consulo.io
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
package consulo.components.impl.stores;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.JDOMExternalizable;
import org.jdom.Element;

/**
 * @author VISTALL
 * @since 27-Feb-17
 */
public class JDOMExternalizableWrapper implements PersistentStateComponent<Element> {
  private final JDOMExternalizable myJDOMExternalizable;

  public JDOMExternalizableWrapper(JDOMExternalizable jdomExternalizable) {
    myJDOMExternalizable = jdomExternalizable;
  }

  @Override
  public Element getState() {
    Element state = new Element("state");
    myJDOMExternalizable.writeExternal(state);
    return state;
  }

  @Override
  public void loadState(Element element) {
    myJDOMExternalizable.readExternal(element);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof JDOMExternalizableWrapper && ((JDOMExternalizableWrapper)obj).myJDOMExternalizable.equals(myJDOMExternalizable);
  }

  @Override
  public int hashCode() {
    return myJDOMExternalizable.hashCode();
  }
}
