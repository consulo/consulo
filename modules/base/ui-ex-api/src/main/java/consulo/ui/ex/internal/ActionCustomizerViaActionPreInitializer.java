/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ui.ex.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.ui.ex.action.*;
import jakarta.inject.Inject;

/**
 * @author VISTALL
 * @since 2026-07-25
 */
@ExtensionImpl
public class ActionCustomizerViaActionPreInitializer implements ActionPreInitializer {
    private final Application myApplication;

    @Inject
    public ActionCustomizerViaActionPreInitializer(Application application) {
        myApplication = application;
    }

    @Override
    public void preload(ActionManager actionManager) {
        ActionCustomizer.Session session = new ActionCustomizer.Session() {
            @Override
            public void unregisterAction(String actionId) {
                actionManager.unregisterAction(actionId);

            }

            @Override
            public AnAction getAction(String actionId) {
                return actionManager.getAction(actionId);
            }

            @Override
            public void registerAction(String actionId, AnAction action) {
                actionManager.registerAction(actionId, action);
            }

            @Override
            public void addToGroup(String groupId, Constraints constraints, String actionId) {
                AnAction action = actionManager.getAction(actionId);
                if (action == null) {
                    throw new IllegalArgumentException("Action by id " + actionId + " not found");
                }

                addToGroup(groupId, constraints, action);
            }

            @Override
            public void addToGroup(String groupId, Constraints constraints, AnAction action) {
                AnAction possibleGroup = actionManager.getAction(groupId);

                if (!(possibleGroup instanceof DefaultActionGroup defaultActionGroup)) {
                    throw new IllegalArgumentException("Action by id " + groupId + " is not group");
                }

                defaultActionGroup.add(action, constraints, actionManager);
            }
        };

        myApplication.getExtensionPoint(ActionCustomizer.class).forEach(c -> c.customize(session));
    }
}
