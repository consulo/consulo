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
package consulo.execution.impl.internal.ui;

import consulo.disposer.Disposable;
import consulo.execution.configuration.ui.CompositeSettingsBuilder;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.execution.configuration.ui.SettingsEditorGroup;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.TabbedLayout;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GroupSettingsBuilder<T> implements CompositeSettingsBuilder<T> {
    private final SettingsEditorGroup<T> myGroup;
    private @Nullable Component myComponent;

    public GroupSettingsBuilder(SettingsEditorGroup<T> group) {
        myGroup = group;
    }

    @Override
    public Collection<SettingsEditor<T>> getEditors() {
        List<SettingsEditor<T>> result = new ArrayList<>();
        for (Pair<LocalizeValue, SettingsEditor<T>> editor : myGroup.getEditors()) {
            result.add(editor.getSecond());
        }
        return result;
    }

    @RequiredUIAccess
    @Override
    public Component createCompoundEditor(Disposable disposable) {
        Component component = myComponent;
        if (component == null) {
            component = doCreateComponent();
            myComponent = component;
        }
        return component;
    }

    @RequiredUIAccess
    private Component doCreateComponent() {
        List<Pair<LocalizeValue, SettingsEditor<T>>> editors = myGroup.getEditors();
        if (editors.isEmpty()) {
            return DockLayout.create();
        }
        if (editors.size() == 1) {
            return editors.get(0).getSecond().getUIComponent();
        }

        TabbedLayout tabbedLayout = TabbedLayout.create();
        for (Pair<LocalizeValue, SettingsEditor<T>> pair : editors) {
            LocalizeValue title = pair.getFirst();

            Tab tab = tabbedLayout.createTab();
            tab.setRenderer((it, presentation) -> presentation.append(title));

            tabbedLayout.addTab(tab, pair.getSecond().getUIComponent());
        }
        return tabbedLayout;
    }
}
