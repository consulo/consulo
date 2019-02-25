/*
 * Copyright 2013-2019 consulo.io
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
package consulo.sandboxPlugin.ide.remoteServer;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.remoteServer.configuration.ServerConfiguration;
import org.jdom.Element;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-02-25
 */
public class SandServerConfiguration extends ServerConfiguration {
  @Override
  public PersistentStateComponent<?> getSerializer() {
    return new PersistentStateComponent<Object>() {
      @Nullable
      @Override
      public Object getState() {
        return new Element("state");
      }

      @Override
      public void loadState(Object state) {

      }
    };
  }
}
