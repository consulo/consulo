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
package consulo.ide.impl.idea.ui.popup;

import consulo.component.ComponentManager;
import consulo.project.Project;
import consulo.util.lang.function.Condition;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.ide.impl.idea.ide.util.gotoByName.ChooseByNameBase;
import consulo.language.editor.completion.lookup.LookupManager;

import java.awt.*;

/**
 * @author yole
 */
public class NotLookupOrSearchCondition implements Condition<ComponentManager> {
    public static NotLookupOrSearchCondition INSTANCE = new NotLookupOrSearchCondition();

    private NotLookupOrSearchCondition() {
    }

    @Override
    public boolean value(final ComponentManager project) {
        final Component focusedComponent = WindowManagerEx.getInstanceEx().getFocusedComponent((Project)project);
        boolean fromQuickSearch = focusedComponent != null && focusedComponent.getParent() instanceof ChooseByNameBase.JPanelProvider;
        return !fromQuickSearch && LookupManager.getInstance((Project)project).getActiveLookup() == null;
    }
}