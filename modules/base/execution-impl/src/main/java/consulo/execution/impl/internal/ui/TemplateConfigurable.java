/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.configuration.ui.SettingsEditorConfigurable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 * @since 2011-10-06
 */
public class TemplateConfigurable extends SettingsEditorConfigurable<RunnerAndConfigurationSettings> {
    private final RunnerAndConfigurationSettings myTemplate;
    private final ConfigurationSettingsEditorWrapperImpl myEditor;

    private @Nullable Component myComponent;

    @RequiredUIAccess
    public TemplateConfigurable(RunnerAndConfigurationSettings template) {
        this(template, new ConfigurationSettingsEditorWrapperImpl(template));
    }

    @RequiredUIAccess
    private TemplateConfigurable(RunnerAndConfigurationSettings template, ConfigurationSettingsEditorWrapperImpl editor) {
        super(editor, template);
        myTemplate = template;
        myEditor = editor;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.ofNullable(myTemplate.getConfiguration().getName());
    }

    @RequiredUIAccess
    @Override
    public @Nullable Component createUIComponent(Disposable parentDisposable) {
        Component component = myComponent;
        if (component == null) {
            Component editorComponent = super.createUIComponent(parentDisposable);
            if (editorComponent == null) {
                return null;
            }
            component = ScrollableLayout.create(editorComponent);
            myComponent = component;
        }
        return component;
    }

    @RequiredUIAccess
    @Override
    public boolean isModified() {
        return super.isModified() || getEditor() != null && myEditor.isModified(myTemplate);
    }

    @RequiredUIAccess
    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        myComponent = null;
    }
}
