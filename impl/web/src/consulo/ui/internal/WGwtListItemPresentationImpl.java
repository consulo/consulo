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
package consulo.ui.internal;

import consulo.ui.ImageRef;
import consulo.ui.ListItemPresentation;
import consulo.ui.TextStyle;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class WGwtListItemPresentationImpl implements ListItemPresentation {
  private WGwtHorizontalLayoutImpl myLayout = new WGwtHorizontalLayoutImpl();

  @Override
  public void append(@NotNull ImageRef... imageRefs) {
    if (imageRefs.length == 0) {
      throw new IllegalArgumentException();
    }

    if (imageRefs.length == 1) {
      myLayout.add(new WGwtImageImpl((WGwtImageRefImpl)imageRefs[0]));
    }
    else {
      myLayout.add(new WGwtLayeredImageImpl(imageRefs));
    }
  }

  @Override
  public void append(@NotNull String text) {
    myLayout.add(new WGwtLabelImpl(text));
  }

  @Override
  public void append(@NotNull String text, @NotNull TextStyle... styles) {
    if (styles[0] == TextStyle.BOLD) {
      myLayout.add(new WGwtHtmlLabelImpl("<b>" + text + "</b>"));
    }
    else {
      append(text);
    }
  }

  public WGwtHorizontalLayoutImpl getLayout() {
    return myLayout;
  }
}
