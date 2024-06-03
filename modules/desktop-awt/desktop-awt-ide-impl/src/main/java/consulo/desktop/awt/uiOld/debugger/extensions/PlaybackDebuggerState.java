/*
 * Copyright 2013-2024 consulo.io
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
package consulo.desktop.awt.uiOld.debugger.extensions;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import jakarta.inject.Singleton;
import org.jdom.Element;

@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "PlaybackDebugger", storages = {@Storage(file = StoragePathMacros.APP_CONFIG + "/other.xml")})
public class PlaybackDebuggerState implements PersistentStateComponent<Element> {
  private static final String ATTR_CURRENT_SCRIPT = "currentScript";
  public String currentScript = "";

  @Override
  public Element getState() {
    final Element element = new Element("playback");
    element.setAttribute(ATTR_CURRENT_SCRIPT, currentScript);
    return element;
  }

  @Override
  public void loadState(Element state) {
    final String path = state.getAttributeValue(ATTR_CURRENT_SCRIPT);
    if (path != null) {
      currentScript = path;
    }
  }
}
