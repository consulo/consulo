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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.ComponentItemRender;
import consulo.ui.HorizontalAlignment;
import consulo.ui.MessageBoxes;
import consulo.ui.Table;
import consulo.ui.TableItemEditor;
import consulo.ui.TextAttribute;
import consulo.ui.TextBox;
import consulo.ui.TriStateCheckBox;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "table", order = "after components")
public class TableUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > Table");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        DockLayout layout = DockLayout.create();

        Map<String, String> values = new TreeMap<>();
        values.put("test1", "1");
        values.put("test2", "3");
        values.put("test3", "5");

        Map<String, ThreeState> states = new TreeMap<>();
        states.put("test1", ThreeState.YES);
        states.put("test2", ThreeState.UNSURE);
        states.put("test3", ThreeState.NO);

        MutableFlatDataModel<String> model = FlatDataModel.of(new ArrayList<>(values.keySet()));

        Table<String> table = Table.create(model);

        table.addColumn(LocalizeValue.localizeTODO("On"), key -> states.getOrDefault(key, ThreeState.NO))
            .setWidth(40)
            .setResizable(false)
            .setRender(ComponentItemRender.reusable(
                () -> TriStateCheckBox.create(LocalizeValue.empty()),
                (checkBox, item) -> checkBox.setValue(item.getValue() == null ? ThreeState.NO : item.getValue())))
            .setEditor(new TableItemEditor<>() {
                @Override
                @RequiredUIAccess
                public ValueComponent<ThreeState> createComponent(String key) {
                    TriStateCheckBox checkBox =
                        TriStateCheckBox.create(LocalizeValue.empty(), states.getOrDefault(key, ThreeState.NO));
                    checkBox.setUnsureEnabled(true);
                    return checkBox;
                }

                @Override
                @RequiredUIAccess
                public void commit(String key, @Nullable ThreeState value) {
                    states.put(key, value == null ? ThreeState.NO : value);
                    model.update(key);
                }
            });

        table.addColumn(LocalizeValue.localizeTODO("Key"), key -> key)
            .setSortable(Comparator.naturalOrder())
            .setWidth(160);

        table.addColumn(LocalizeValue.localizeTODO("Value"), values::get)
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder())
            .setRender((presentation, item) -> presentation.append(
                String.valueOf(item.getValue()),
                item.isSelected() ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR
            ))
            .setEditor(new TableItemEditor<>() {
                @Override
                @RequiredUIAccess
                public ValueComponent<String> createComponent(String key) {
                    return TextBox.create(values.get(key));
                }

                @Override
                @RequiredUIAccess
                public void commit(String key, @Nullable String value) {
                    values.put(key, value == null ? "" : value);
                    model.update(key);
                }
            });

        table.setAllowMultipleSelect(true);
        table.setSpeedSearchConverter(key -> key);
        table.addSelectListener(event -> MessageBoxes.okInfo(LocalizeValue.of("Selected: " + event.getValues().size())).showAsync());

        layout.center(ScrollableLayout.create(table));

        return layout;
    }
}
