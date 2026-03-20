/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.application.options.editor;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.ConfigurableAdapter;
import consulo.configurable.StandardConfigurableIds;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class AutoImportOptionsConfigurable extends ConfigurableAdapter implements ApplicationConfigurable {
  
  @Override
  public String getId() {
    return "editor.preferences.import";
  }

  @Override
  public @Nullable String getParentId() {
    return StandardConfigurableIds.EDITOR_GROUP;
  }

  
  
  @Override
  public LocalizeValue getDisplayName() {
    return ApplicationLocalize.autoImport();
  }
}
