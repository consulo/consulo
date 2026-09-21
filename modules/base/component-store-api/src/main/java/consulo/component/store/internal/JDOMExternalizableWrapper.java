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
package consulo.component.store.internal;

import consulo.component.persist.PersistentStateComponent;
import consulo.util.xml.serializer.JDOMExternalizable;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2017-02-27
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
    public boolean equals(@Nullable Object obj) {
        return obj == this
            || obj instanceof JDOMExternalizableWrapper that && that.myJDOMExternalizable.equals(myJDOMExternalizable);
    }

    @Override
    public int hashCode() {
        return myJDOMExternalizable.hashCode();
    }
}
