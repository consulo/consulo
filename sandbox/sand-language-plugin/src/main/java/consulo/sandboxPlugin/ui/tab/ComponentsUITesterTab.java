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
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.AdvancedLabel;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.DatePicker;
import consulo.ui.HtmlLabel;
import consulo.ui.HtmlView;
import consulo.ui.Hyperlink;
import consulo.ui.IntBox;
import consulo.ui.IntSlider;
import consulo.ui.Label;
import consulo.ui.MessageBoxes;
import consulo.ui.MultiSelectComboBox;
import consulo.ui.PasswordBox;
import consulo.ui.ProgressBar;
import consulo.ui.ProgressBarStyle;
import consulo.ui.TextAttribute;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.TextBoxWithExtensions;
import consulo.ui.ToggleSwitch;
import consulo.ui.TriStateCheckBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.font.Font;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.StandardColors;
import consulo.util.lang.ThreeState;

import java.util.Date;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionImpl(id = "components", order = "after layouts")
public class ComponentsUITesterTab implements UITesterTab {
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        VerticalLayout layout = VerticalLayout.create();

        FileChooserTextBoxBuilder builder = FileChooserTextBoxBuilder.create(null);
        layout.add(builder.build());

        ToggleSwitch toggleSwitch = ToggleSwitch.create(true);
        toggleSwitch.addValueListener(event -> MessageBoxes.okInfo(LocalizeValue.of("toggle")).showAsync());

        CheckBox checkBox = CheckBox.create(LocalizeValue.of("Check box"));
        checkBox.addValueListener(event -> MessageBoxes.okInfo(LocalizeValue.of("checkBox")).showAsync());
        checkBox.setToolTipText(LocalizeValue.of("Some Tooltip"));

        layout.add(AdvancedLabel.create().updatePresentation(presentation -> {
            presentation.append(LocalizeValue.of("Advanced "), TextAttribute.REGULAR_BOLD);
            presentation.append(
                LocalizeValue.of("Label"),
                new TextAttribute(Font.PLAIN, StandardColors.RED, StandardColors.BLACK)
            );
        }));

        layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("Toggle Switch"))).add(toggleSwitch).add(checkBox));

        TriStateCheckBox triStateCheckBox = TriStateCheckBox.create(LocalizeValue.of("Tri state"));
        triStateCheckBox.addValueListener(
            event -> MessageBoxes.okInfo(LocalizeValue.of("triStateCheckBox " + event.getValue())).showAsync()
        );

        TriStateCheckBox twoStateCheckBox = TriStateCheckBox.create(LocalizeValue.of("Unsure disabled"), ThreeState.UNSURE);
        twoStateCheckBox.setUnsureEnabled(false);
        twoStateCheckBox.addValueListener(
            event -> MessageBoxes.okInfo(LocalizeValue.of("twoStateCheckBox " + event.getValue())).showAsync()
        );

        layout.add(HorizontalLayout.create()
            .add(Label.create(LocalizeValue.of("TriStateCheckBox")))
            .add(triStateCheckBox)
            .add(twoStateCheckBox));

        layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("Password"))).add(PasswordBox.create()));

        ProgressBar spinnerBar = ProgressBar.create();
        spinnerBar.addStyle(ProgressBarStyle.SPINNER);
        spinnerBar.setIndeterminate(true);

        layout.add(HorizontalLayout.create()
            .add(Label.create(LocalizeValue.of("Spinner Progress")))
            .add(spinnerBar));

        IntSlider intSlider = IntSlider.create(3);
        intSlider.addValueListener(event -> MessageBoxes.okInfo(LocalizeValue.of("intSlider " + event.getValue())).showAsync());
        layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("IntSlider"))).add(intSlider));

        IntBox intBox = IntBox.create(5).withRange(0, 100).withStep(5);
        intBox.addValueListener(event -> MessageBoxes.okInfo(LocalizeValue.of("intBox " + event.getValue())).showAsync());
        layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("IntBox"))).add(intBox));

        DatePicker datePicker = DatePicker.create();
        datePicker.setValue(new Date());
        datePicker.addValueListener(event -> MessageBoxes.okInfo(LocalizeValue.of("datePicker " + event.getValue())).showAsync());
        layout.add(HorizontalLayout.create().add(Label.create(LocalizeValue.of("DatePicker"))).add(datePicker));

        MultiSelectComboBox<String> multiSelectComboBox = MultiSelectComboBox.create("Java", "Kotlin", "Scala", "Groovy", "Clojure");
        multiSelectComboBox.setPlaceholder(LocalizeValue.of("Pick languages"));
        multiSelectComboBox.setValue(List.of("Java", "Scala"));
        Label multiSelectValue = Label.create(LocalizeValue.of(multiSelectComboBox.getValue().toString()));
        multiSelectComboBox.addValueListener(event -> multiSelectValue.setText(LocalizeValue.of(event.getValue().toString())));
        layout.add(HorizontalLayout.create()
            .add(Label.create(LocalizeValue.of("MultiSelectComboBox")))
            .add(multiSelectComboBox)
            .add(multiSelectValue));

        MultiSelectComboBox<String> summaryComboBox = MultiSelectComboBox.create("Java", "Kotlin", "Scala", "Groovy", "Clojure");
        summaryComboBox.setPlaceholder(LocalizeValue.of("Languages"));
        summaryComboBox.setSummaryRenderer(items -> LocalizeValue.of("Languages: " + String.join(", ", items)));
        summaryComboBox.setValue(List.of("Java", "Kotlin", "Scala", "Groovy", "Clojure"));
        layout.add(HorizontalLayout.create()
            .add(Label.create(LocalizeValue.of("MultiSelectComboBox (summary)")))
            .add(summaryComboBox));

        layout.add(HtmlLabel.create(LocalizeValue.of("<b>Html</b> <i>Label</i>")));

        TextBoxWithExtensions textBoxWithExtensions = TextBoxWithExtensions.create("with extensions");
        textBoxWithExtensions.addLastExtension(new TextBoxWithExtensions.Extension(
            false,
            PlatformIconGroup.actionsFind(),
            null,
            event -> MessageBoxes.okInfo(LocalizeValue.of("extension clicked")).showAsync()
        ));
        layout.add(HorizontalLayout.create()
            .add(Label.create(LocalizeValue.of("TextBox With Extensions")))
            .add(textBoxWithExtensions));

        TextBoxWithExpandAction textBoxWithExpandAction = TextBoxWithExpandAction.create(
            null,
            "Edit Lines",
            text -> List.of(text.split(";")),
            lines -> String.join(";", lines)
        );
        textBoxWithExpandAction.setValue("one;two;three");
        layout.add(HorizontalLayout.create()
            .add(Label.create(LocalizeValue.of("TextBox With Expand")))
            .add(textBoxWithExpandAction));

        layout.add(Hyperlink.create(
            LocalizeValue.localizeTODO("Some Link"),
            (e) -> MessageBoxes.okInfo(LocalizeValue.of("Clicked!!!")).showAsync()
        ));

        HtmlView component = HtmlView.create();
        component.render(new HtmlView.RenderData("<html><body><b>Some Bold Text</b> Test</body></html>"));
        layout.add(component);
        return layout;
    }
}
