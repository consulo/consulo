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
package consulo.web.internal.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.theme.lumo.LumoUtility;
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.image.Image;
import consulo.web.internal.ui.image.WebImageConverter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebItemPresentationImpl implements TextItemPresentation {
  private Image myIcon;
  private List<Component> myFragments = new ArrayList<>();

  @Nonnull
  @Override
  public TextItemPresentation withIcon(@Nullable Image image) {
    myIcon = image;

    after();
    return this;
  }

  @Override
  public void append(@Nonnull LocalizeValue text, @Nonnull TextAttribute textAttribute) {
    Span span = new Span(text.get());
    myFragments.add(span);

    after();
  }

  @Override
  public void clearText() {
    myFragments.clear();

    after();
  }

  public Component toComponent() {
    Span span = new Span();
    span.addClassName("web-icon");
    if (myIcon != null) {
      Component image = WebImageConverter.getImageCanvas(myIcon);
      image.addClassName(LumoUtility.Margin.Right.SMALL);
      span.add(image);
    }
    span.add(myFragments.toArray(Component[]::new));
    return span;
  }

  protected void after() {
  }
}
