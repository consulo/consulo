/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.template.postfix.completion;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.util.IconUtil;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupActionProvider;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementAction;
import consulo.language.editor.postfixTemplate.PostfixTemplate;
import consulo.language.editor.postfixTemplate.PostfixTemplatesSettings;
import consulo.project.Project;

import java.util.function.Consumer;

@ExtensionImpl
public class PostfixTemplateLookupActionProvider implements LookupActionProvider {
  @Override
  public void fillActions(LookupElement element, final Lookup lookup, Consumer<LookupElementAction> consumer) {
    if (element instanceof PostfixTemplateLookupElement) {
      final PostfixTemplateLookupElement templateLookupElement = (PostfixTemplateLookupElement)element;
      final PostfixTemplate template = templateLookupElement.getPostfixTemplate();

      consumer.accept(new LookupElementAction(IconUtil.getEditIcon(), "Edit postfix templates settings") {
        @Override
        public Result performLookupAction() {
          final Project project = lookup.getEditor().getProject();
          assert project != null;
          ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
              if (project.isDisposed()) return;

              // TODO show correct settings
              //PostfixTemplatesConfigurable configurable = new PostfixTemplatesConfigurable();
              //PostfixTemplatesChildConfigurable childConfigurable = configurable.findConfigurable(templateLookupElement.getProvider());
              //if(childConfigurable == null) {
              //  return;
              //}
              //ShowSettingsUtil.getInstance().editConfigurable(project, childConfigurable, () -> childConfigurable.focusTemplate(template));
            }
          });
          return Result.HIDE_LOOKUP;
        }
      });

      final PostfixTemplatesSettings settings = PostfixTemplatesSettings.getInstance();
      if (settings.isTemplateEnabled(template, templateLookupElement.getProvider())) {
        consumer.accept(new LookupElementAction(AllIcons.Actions.Cancel, String.format("Disable '%s' template", template.getKey())) {
          @Override
          public Result performLookupAction() {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
              @Override
              public void run() {
                settings.disableTemplate(template, templateLookupElement.getProvider());
              }
            });
            return Result.HIDE_LOOKUP;
          }
        });
      }
    }
  }
}
