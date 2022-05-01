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

import consulo.language.editor.completion.lookup.Lookup;
import consulo.ide.impl.idea.codeInsight.lookup.LookupActionProvider;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.ide.impl.idea.codeInsight.lookup.LookupElementAction;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.ide.impl.idea.util.Consumer;
import consulo.ide.impl.idea.util.IconUtil;

/**
 * @author peter
 */
public class LiveTemplateLookupActionProvider implements LookupActionProvider {
  @Override
  public void fillActions(LookupElement element, final Lookup lookup, Consumer<LookupElementAction> consumer) {
    if (element instanceof LiveTemplateLookupElementImpl) {
      final TemplateImpl template = ((LiveTemplateLookupElementImpl)element).getTemplate();
      final TemplateImpl templateFromSettings = TemplateSettings.getInstance().getTemplate(template.getKey(), template.getGroupName());

      if (templateFromSettings != null) {
        consumer.consume(new LookupElementAction(IconUtil.getEditIcon(), "Edit live template settings") {
          @Override
          public Result performLookupAction() {
            final Project project = lookup.getEditor().getProject();
            assert project != null;
            ApplicationManager.getApplication().invokeLater(new Runnable() {
              @Override
              public void run() {
                if (project.isDisposed()) return;

                final LiveTemplatesConfigurable configurable = new LiveTemplatesConfigurable();
                ShowSettingsUtil.getInstance().editConfigurable(project, configurable, new Runnable() {
                  @Override
                  public void run() {
                    configurable.getTemplateListPanel().editTemplate(template);
                  }
                });
              }
            });
            return Result.HIDE_LOOKUP;
          }
        });


        consumer.consume(new LookupElementAction(AllIcons.Actions.Cancel, String.format("Disable '%s' template", template.getKey())) {
          @Override
          public Result performLookupAction() {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
              @Override
              public void run() {
                templateFromSettings.setDeactivated(true);
              }
            });
            return Result.HIDE_LOOKUP;
          }
        });
      }
    }
  }
}
