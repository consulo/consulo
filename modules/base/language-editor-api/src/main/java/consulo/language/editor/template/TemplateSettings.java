/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.template;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import org.jspecify.annotations.Nullable;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 18-Jul-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface TemplateSettings {
  
  static TemplateSettings getInstance() {
    return Application.get().getInstance(TemplateSettings.class);
  }

  @Nullable
  Template getTemplateById(String id);

  
  Collection<? extends Template> getTemplates();

  
  Collection<? extends Template> getTemplates(String templateKey);

  @Nullable
  Template getTemplate(String key, String group);

  char getDefaultShortcutChar();

  char getShortcutChar(Template template);
}
