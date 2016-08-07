/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwtUI.client.ui;

import com.google.gwt.user.client.ui.InlineHTML;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Map;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class GwtHtmlLabelImpl extends InlineHTML implements InternalGwtComponent {
  public GwtHtmlLabelImpl() {
    setHorizontalAlignment(ALIGN_CENTER);
  }

  @Override
  public void updateState(@NotNull Map<String, Serializable> map) {
    final String text = (String)map.get("text");
    if (text != null) {
      setHTML(text);
    }
  }
}
