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
package consulo.continuousIntegration.impl.internal.run;

import consulo.continuousIntegration.CIParameterDefinition;
import consulo.continuousIntegration.CIParameterValue;
import consulo.continuousIntegration.ui.CIParameterEditor;
import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.PasswordBox;
import consulo.ui.TextArea;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

/**
 * Editors of the standard {@link consulo.continuousIntegration.CIParameterKind}s, used when no
 * {@link consulo.continuousIntegration.ui.CIParameterEditorProvider} accepts a parameter.
 *
 * @since 2026-11-09
 */
public final class CIStandardParameterEditors {
    private static final class SimpleEditor implements CIParameterEditor {
        private final Component myComponent;
        private final Supplier<@Nullable CIParameterValue> myValue;

        private SimpleEditor(Component component, Supplier<@Nullable CIParameterValue> value) {
            myComponent = component;
            myValue = value;
        }

        @Override
        public Component getComponent() {
            return myComponent;
        }

        @RequiredUIAccess
        @Override
        public @Nullable CIParameterValue getValue() {
            return myValue.get();
        }
    }

    private CIStandardParameterEditors() {
    }

    @RequiredUIAccess
    public static CIParameterEditor create(Project project, CIParameterDefinition definition, Disposable uiDisposable) {
        String defaultValue = StringUtil.notNullize(definition.defaultValue());
        return switch (definition.kind()) {
            case STRING, OTHER -> {
                TextBox textBox = TextBox.create(defaultValue);
                yield new SimpleEditor(textBox, () -> CIParameterValue.text(StringUtil.notNullize(textBox.getValue())));
            }
            case TEXT -> {
                TextArea textArea = TextArea.create(defaultValue);
                textArea.setMinRows(3);
                yield new SimpleEditor(textArea, () -> CIParameterValue.text(StringUtil.notNullize(textArea.getValue())));
            }
            case BOOLEAN -> {
                CheckBox checkBox = CheckBox.create(LocalizeValue.empty(), Boolean.parseBoolean(defaultValue.trim()));
                yield new SimpleEditor(checkBox, () -> CIParameterValue.text(String.valueOf(Boolean.TRUE.equals(checkBox.getValue()))));
            }
            case CHOICE -> createChoice(definition.choices(), defaultValue);
            case PASSWORD -> {
                // the server keeps the default secret: an empty box sends nothing, so the default stays
                PasswordBox passwordBox = PasswordBox.create();
                yield new SimpleEditor(passwordBox, () -> {
                    String value = passwordBox.getValue();
                    return StringUtil.isEmpty(value) ? null : CIParameterValue.text(value);
                });
            }
            case FILE -> {
                FileChooserTextBoxBuilder.Controller controller = FileChooserTextBoxBuilder.create(project)
                    .uiDisposable(uiDisposable)
                    .build();
                yield new SimpleEditor(controller.getComponent(), () -> toFile(controller.getValue()));
            }
        };
    }

    @RequiredUIAccess
    private static CIParameterEditor createChoice(List<String> choices, String defaultValue) {
        ComboBox<String> comboBox = ComboBox.create(choices);
        if (!choices.isEmpty()) {
            comboBox.setValue(choices.contains(defaultValue) ? defaultValue : choices.get(0));
        }
        return new SimpleEditor(comboBox, () -> {
            String value = comboBox.getValue();
            return value == null ? null : CIParameterValue.text(value);
        });
    }

    private static @Nullable CIParameterValue toFile(String path) {
        if (path.isBlank()) {
            return null;
        }
        try {
            return CIParameterValue.file(Path.of(path.trim()));
        }
        catch (InvalidPathException e) {
            return null;
        }
    }
}
