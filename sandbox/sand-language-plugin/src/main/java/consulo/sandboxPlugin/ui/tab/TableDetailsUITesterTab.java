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
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Table;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.model.MutableFlatDataModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-11-10
 */
@ExtensionImpl(id = "tableDetails", order = "after table")
public class TableDetailsUITesterTab implements UITesterTab {
    private static final class Parameter {
        private String myName;
        private String myType;
        private String myDefaultValue;

        private Parameter(String name, String type, String defaultValue) {
            myName = name;
            myType = type;
            myDefaultValue = defaultValue;
        }
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > Table Details");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        MutableFlatDataModel<Parameter> model = FlatDataModel.of(new ArrayList<>(List.of(
            new Parameter("project", "Project", ""),
            new Parameter("file", "VirtualFile", "null"),
            new Parameter("offset", "int", "0"),
            new Parameter("force", "boolean", "false")
        )));

        Table<Parameter> table = Table.create(model);
        table.addColumn(LocalizeValue.localizeTODO("Name"), parameter -> parameter.myName).setWidth(160);
        table.addColumn(LocalizeValue.localizeTODO("Type"), parameter -> parameter.myType).setWidth(160);
        table.addColumn(LocalizeValue.localizeTODO("Default Value"), parameter -> parameter.myDefaultValue);

        table.setDetailsRenderer(parameter -> createDetails(model, parameter));
        table.addSelectListener(event -> {
            for (Parameter parameter : model) {
                if (!event.getValues().contains(parameter)) {
                    table.setDetailsVisible(parameter, false);
                }
            }
            for (Parameter parameter : event.getValues()) {
                table.setDetailsVisible(parameter, true);
            }
        });

        Button add = Button.create(LocalizeValue.localizeTODO("Add"), event -> {
            Parameter parameter = new Parameter("parameter" + (model.getSize() + 1), "String", "\"\"");
            model.add(parameter);
            table.select(parameter);
        });
        Button remove = Button.create(LocalizeValue.localizeTODO("Remove"), event -> {
            Parameter selected = table.getSelectedItem();
            if (selected != null) {
                model.remove(selected);
            }
        });

        DockLayout layout = DockLayout.create();
        layout.top(DockLayout.create()
            .left(Label.create(LocalizeValue.localizeTODO("Select a row to edit it in its details")))
            .right(HorizontalLayout.create().add(add).add(remove)));
        layout.center(ScrollableLayout.create(table));
        return layout;
    }

    @RequiredUIAccess
    private static Component createDetails(MutableFlatDataModel<Parameter> model, Parameter parameter) {
        TextBox name = TextBox.create(parameter.myName);
        name.addValueListener(event -> {
            parameter.myName = event.getValue();
            model.update(parameter);
        });

        TextBox type = TextBox.create(parameter.myType);
        type.addValueListener(event -> {
            parameter.myType = event.getValue();
            model.update(parameter);
        });

        TextBox defaultValue = TextBox.create(parameter.myDefaultValue);
        defaultValue.addValueListener(event -> {
            parameter.myDefaultValue = event.getValue();
            model.update(parameter);
        });

        VerticalLayout details = VerticalLayout.create();
        details.add(LabeledLayout.create(LocalizeValue.localizeTODO("Name:"), name));
        details.add(LabeledLayout.create(LocalizeValue.localizeTODO("Type:"), type));
        details.add(LabeledLayout.create(LocalizeValue.localizeTODO("Default value:"), defaultValue));
        return details;
    }
}
