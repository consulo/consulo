/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.gwt.client.ui;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
public class GwtLayeredImageImpl extends FlowPanel implements InternalGwtComponent {
  public GwtLayeredImageImpl() {
    setStyleName("gwtLayeredImage");
  }

  @Override
  public void updateState(@NotNull Map<String, Object> map) {
    final int width = (Integer)map.get("width");
    setWidth(width + "px");
    final int height = (Integer)map.get("height");
    setHeight(height + "px");

    final int size = (Integer)map.get("size");
    for (int i = 0; i < size; i++) {
      final String url = (String)map.get("url" + i);

      Image image = new Image(url);
      add(image);
    }
  }
}
