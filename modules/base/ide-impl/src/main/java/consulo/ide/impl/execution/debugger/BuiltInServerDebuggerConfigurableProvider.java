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
package consulo.ide.impl.execution.debugger;

import consulo.annotation.component.ExtensionImpl;
import consulo.builtinWebServer.impl.BuiltInServerConfigurable;
import consulo.configurable.Configurable;
import consulo.execution.debug.setting.DebuggerSettingsCategory;
import consulo.execution.debug.setting.XDebuggerSettings;
import jakarta.inject.Inject;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
* @author VISTALL
* @since 20-Jun-22
*/
@ExtensionImpl
final class BuiltInServerDebuggerConfigurableProvider extends XDebuggerSettings<Element> {
  @Inject
  public BuiltInServerDebuggerConfigurableProvider() {
    super("buildin-server");
  }

  @Nonnull
  @Override
  public Collection<? extends Configurable> createConfigurables(@Nonnull DebuggerSettingsCategory category) {
    if (category == DebuggerSettingsCategory.GENERAL) {
      return List.of(new BuiltInServerConfigurable());
    }
    return List.of();
  }

  @Override
  public Element getState() {
    return null;
  }

  @Override
  public void loadState(Element state) {

  }
}
