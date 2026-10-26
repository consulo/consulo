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
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputValidators;
import consulo.ui.Label;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.cursor.StandardCursors;
import consulo.ui.event.details.KeyCode;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "inputs", order = "after messageBoxes")
public class InputsUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Inputs");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();

        layout.add(Button.create(
            LocalizeValue.of("Text"),
            event -> InputBoxBuilder.text()
                .title(LocalizeValue.of("Rename"))
                .text(LocalizeValue.of("New name:"))
                .value("current")
                .showAsync()
                .whenComplete((answer, error) -> UITesterTabUtil.report("text", answer, error))
        ));

        layout.add(Button.create(
            LocalizeValue.of("Text, validated non-empty"),
            event -> InputBoxBuilder.text()
                .text(LocalizeValue.of("Name (required):"))
                .validator(InputValidators.nonEmpty(LocalizeValue.of("A name is required")))
                .showAsync()
                .whenComplete((answer, error) -> UITesterTabUtil.report("validated", answer, error))
        ));

        layout.add(Button.create(
            LocalizeValue.of("Integer, ranged 1..64"),
            event -> InputBoxBuilder.integer()
                .text(LocalizeValue.of("How many threads?"))
                .value(4)
                .setupComponent(box -> box.withRange(1, 64))
                .showAsync()
                .whenComplete((answer, error) -> UITesterTabUtil.report("integer", answer, error))
        ));

        layout.add(Button.create(
            LocalizeValue.of("Password"),
            event -> InputBoxBuilder.password()
                .text(LocalizeValue.of("Master password:"))
                .showAsync()
                .whenComplete((answer, error) -> UITesterTabUtil.report(
                    "password",
                    answer == null ? null : "*".repeat(answer.length()),
                    error
                ))
        ));

        layout.add(Button.create(
            LocalizeValue.of("One of a list"),
            event -> InputBoxBuilder.items(List.of(StandardCursors.values()))
                .text(LocalizeValue.of("Pick a cursor:"))
                .setupComponent(box -> box.setRenderer((presentation, item) -> presentation.append(item.getValue().name())))
                .showAsync()
                .whenComplete((answer, error) -> UITesterTabUtil.report("items", answer, error))
        ));

        // Enter has to reach the key listener of a text box with history, whose keys land in the editor of a combo
        List<String> history = new ArrayList<>();
        Label entered = Label.create(LocalizeValue.of("Type and press Enter:"));
        TextBoxWithHistory withHistory = TextBoxWithHistory.create();
        withHistory.addKeyPressedListener(event -> {
            KeyCode keyCode = event.getInputDetails().getKeyCode();
            if (keyCode == null || keyCode.key() != KeyCode.ENTER.key()) {
                return;
            }
            String text = String.valueOf(withHistory.getValue());
            history.remove(text);
            history.add(0, text);
            withHistory.setHistory(history);
            entered.setText(LocalizeValue.of("Entered: " + text));
        });
        layout.add(entered);
        layout.add(withHistory);

        return layout;
    }
}
