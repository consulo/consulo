/*
 * Copyright 2013-2018 consulo.io
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

import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author VISTALL
 * @since 2018-05-11
 */
public class GwtHyperlinkImpl extends HorizontalPanel {
  private Anchor myAnchor;
  private Widget myImage;
  private int myWidth;
  private int myHeight;

  public GwtHyperlinkImpl() {
    setStyleName("ui-hyperlink-root");

    myAnchor = new Anchor();
    myAnchor.setStyleName("ui-hyperlink");

    rebuild();
  }

  public Anchor getAnchor() {
    return myAnchor;
  }

  public void setImage(Widget image, int width, int height) {
    myImage = image;
    myWidth = width;
    myHeight = height;
  }

  public void rebuild() {
    clear();

    if (myImage != null) {
      add(myImage);
    }
    add(myAnchor);
  }
}
