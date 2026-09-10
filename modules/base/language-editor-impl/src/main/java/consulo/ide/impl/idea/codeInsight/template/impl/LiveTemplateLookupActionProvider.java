/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.template.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupActionProvider;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementAction;
import consulo.language.editor.impl.internal.template.LiveTemplateLookupElementImpl;
import consulo.language.editor.impl.internal.template.TemplateImpl;
import consulo.language.editor.impl.internal.template.TemplateSettingsImpl;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.template.Template;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;

import java.util.function.Consumer;

/**
 * @author peter
 */
@ExtensionImpl
public class LiveTemplateLookupActionProvider implements LookupActionProvider {
    @Override
    public void fillActions(LookupElement element, final Lookup lookup, Consumer<LookupElementAction> consumer) {
        if (!(element instanceof LiveTemplateLookupElementImpl lookupElement)) {
            return;
        }
        Template template = lookupElement.getTemplate();
        TemplateImpl templateFromSettings =
            TemplateSettingsImpl.getInstanceImpl().getTemplate(template.getKey(), template.getGroupName());

        if (templateFromSettings != null) {
            consumer.accept(new LookupElementAction(
                PlatformIconGroup.actionsEdit(),
                CodeInsightLocalize.actionTextEditLiveTemplateSettings()
            ) {
                @Override
                public Result performLookupAction() {
                    Project project = lookup.getEditor().getProject();
                    assert project != null;
                    project.getApplication().invokeLater(() -> {
                        if (project.isDisposed()) {
                            return;
                        }

                        LiveTemplatesConfigurable configurable = new LiveTemplatesConfigurable();
                        LanguageEditorInternalHelper.getInstance().editConfigurable(
                            project,
                            configurable,
                            () -> configurable.getTemplateListPanel().editTemplate((TemplateImpl) template)
                        );
                    });
                    return Result.HIDE_LOOKUP;
                }
            });

            consumer.accept(new LookupElementAction(
                PlatformIconGroup.actionsCancel(),
                CodeInsightLocalize.actionTextDisableLiveTemplate(template.getKey())
            ) {
                @Override
                public Result performLookupAction() {
                    Application.get().invokeLater(() -> templateFromSettings.setDeactivated(true));
                    return Result.HIDE_LOOKUP;
                }
            });
        }
    }
}
