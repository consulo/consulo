/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;

import org.jspecify.annotations.Nullable;

/**
 * @author Eugene.Kudelevsky
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CustomLiveTemplate {
  ExtensionPointName<CustomLiveTemplate> EP_NAME = ExtensionPointName.create(CustomLiveTemplate.class);

  @Nullable String computeTemplateKey(CustomTemplateCallback callback);

  boolean isApplicable(CustomTemplateCallback callback, int offset, boolean wrapping);

  boolean supportsWrapping();

  void expand(String key, CustomTemplateCallback callback);

  void wrap(String selection, CustomTemplateCallback callback);

  
  String getTitle();

  char getShortcut();
}
