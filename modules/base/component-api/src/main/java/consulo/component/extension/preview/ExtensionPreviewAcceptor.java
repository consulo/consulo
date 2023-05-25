/*
 * Copyright 2013-2023 consulo.io
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
package consulo.component.extension.preview;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.container.plugin.PluginExtensionPreview;

/**
 * By default - all extensions previews checks by simple equals ID check, but some extensions can provide own way for it
 *
 * @author VISTALL
 * @since 25/05/2023
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ExtensionPreviewAcceptor<Api> {
  static ExtensionPreviewAcceptor<Object> DEFAULT = new ExtensionPreviewAcceptor<Object>() {
    @Override
    public boolean accept(PluginExtensionPreview pluginPreview, PluginExtensionPreview featurePreview) {
      return pluginPreview.equals(featurePreview);
    }

    @Override
    public Class<Object> getApiClass() {
      throw new UnsupportedOperationException();
    }
  };

  boolean accept(PluginExtensionPreview pluginPreview, PluginExtensionPreview featurePreview);

  Class<Api> getApiClass();
}
