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

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class GwtLabelImpl extends GwtHorizontalLayoutImpl {
  private Label myLabel;

  public GwtLabelImpl() {
    myLabel = new GwtHtmlLabelImpl();
    myLabel.setHorizontalAlignment(ALIGN_LEFT);
    myLabel.setStyleName("ui-label");

    setSpacing(4);
    
    add(myLabel);
  }

  public void setText(String text) {
    myLabel.setText(text);
  }

  public void setIcon(@Nullable Widget iconWidget) {
    clear();

    if (iconWidget == null) {
      add(myLabel);
    }
    else {
      add(iconWidget);
      add(myLabel);
    }
  }

  public Label getLabel() {
    return myLabel;
  }
}
