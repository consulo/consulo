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
package consulo.externalService.impl.internal.plugin;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.progress.PerformInBackgroundOption;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.util.xml.serializer.XmlSerializer;
import jakarta.inject.Singleton;
import org.jdom.Element;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@State(name = "PluginManagerConfigurable", storages = @Storage("plugin_ui.xml"))
public class PluginManagerUISettings implements PersistentStateComponent<Element>, PerformInBackgroundOption {
  public boolean UPDATE_IN_BACKGROUND = false;

  public static PluginManagerUISettings getInstance() {
    return Application.get().getInstance(PluginManagerUISettings.class);
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    XmlSerializer.serializeInto(this, element);
    return element;
  }

  @Override
  public void loadState(final Element element) {
    XmlSerializer.deserializeInto(this, element);
  }

  @Override
  public boolean shouldStartInBackground() {
    return UPDATE_IN_BACKGROUND;
  }

  @Override
  public void processSentToBackground() {
    UPDATE_IN_BACKGROUND = true;
  }
}
