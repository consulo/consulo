/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.ide.actionMacro;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-09-05
 */
@State(name = "ActionMacroManager", storages = @Storage("macros.xml"))
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION)
@ServiceImpl
public class ActionMacroManagerState implements JDOMExternalizable {
    private static final String ELEMENT_MACRO = "macro";
    private ArrayList<ActionMacro> myMacros = new ArrayList<>();

    private final Provider<ActionMacroManager> myActionMacroManagerProvider;

    @Inject
    public ActionMacroManagerState(Provider<ActionMacroManager> actionMacroManagerProvider) {
        myActionMacroManagerProvider = actionMacroManagerProvider;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        myMacros = new ArrayList<>();
        final List macros = element.getChildren(ELEMENT_MACRO);
        for (final Object o : macros) {
            Element macroElement = (Element) o;
            ActionMacro macro = new ActionMacro();
            macro.readExternal(macroElement);
            myMacros.add(macro);
        }

        myActionMacroManagerProvider.get().registerActions(getAllMacros());
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        for (ActionMacro macro : myMacros) {
            Element macroElement = new Element(ELEMENT_MACRO);
            macro.writeExternal(macroElement);
            element.addContent(macroElement);
        }
    }

    public void removeMacro(String name) {
        for (int i = 0; i < myMacros.size(); i++) {
            ActionMacro macro = myMacros.get(i);
            if (name.equals(macro.getName())) {
                myMacros.remove(i);
                break;
            }
        }
    }

    public void reinitMacros() {
        myMacros = new ArrayList<>();
    }

    public ArrayList<ActionMacro> getMacros() {
        return myMacros;
    }

    public ActionMacro[] getAllMacros() {
        return myMacros.toArray(new ActionMacro[myMacros.size()]);
    }
}
