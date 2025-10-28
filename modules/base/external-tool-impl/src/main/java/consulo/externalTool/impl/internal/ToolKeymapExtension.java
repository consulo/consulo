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
package consulo.externalTool.impl.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalTool.impl.internal.localize.ExternalToolLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.keymap.KeymapGroupFactory;
import jakarta.inject.Inject;

@ExtensionImpl
public class ToolKeymapExtension extends BaseToolKeymapExtension {
  private final ToolManager myToolManager;

  @Inject
  public ToolKeymapExtension(KeymapGroupFactory keymapGroupFactory) {
      super(keymapGroupFactory);
      myToolManager = ToolManager.getInstance();
  }

  @Override
  protected String getActionIdPrefix() {
    return Tool.ACTION_ID_PREFIX;
  }

  @Override
  protected String getGroupByActionId(String id) {
    return myToolManager.getGroupByActionId(id);
  }

  @Override
  protected LocalizeValue getGroupName() {
      return ExternalToolLocalize.toolsSettingsTitle();
  }
}
