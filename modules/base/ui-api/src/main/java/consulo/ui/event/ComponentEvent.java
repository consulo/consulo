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
package consulo.ui.event;

import consulo.ui.Component;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.util.dataholder.UserDataHolderBase;

/**
 * The user data is how one listener leaves something for the ones dispatched after it. The listeners of an
 * event are open to plugins, so there is deliberately no consumed flag - a veto in one plugin's hands would
 * switch every other listener off - and what a listener wants the others to know it says in the data instead.
 * <p>
 * The details are never null - an event built without any is taken for one no user input drove and carries
 * {@link ProgrammaticInputDetails}.
 *
 * @author VISTALL
 * @since 2024-09-10
 */
public class ComponentEvent<C extends Component> extends UserDataHolderBase {
    private final C myComponent;
    private final InputDetails myInputDetails;

    public ComponentEvent(C component) {
        this(component, ProgrammaticInputDetails.INSTANCE);
    }

    public ComponentEvent(C component, InputDetails inputDetails) {
        myComponent = component;
        myInputDetails = inputDetails;
    }

    public InputDetails getInputDetails() {
        return myInputDetails;
    }

    public C getComponent() {
        return myComponent;
    }
}
