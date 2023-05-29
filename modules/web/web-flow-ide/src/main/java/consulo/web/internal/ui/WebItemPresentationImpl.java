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
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2019-02-18
 */
public class WebItemPresentationImpl implements TextItemPresentation {
  private List<Component> myFragments = new ArrayList<>();

  @Nonnull
  @Override
  public TextItemPresentation withIcon(@Nullable Image image) {

    after();
    return this;
  }

  @Override
  public void append(@Nonnull LocalizeValue text, @Nonnull TextAttribute textAttribute) {
    myFragments.add(new Span(text.get()));

    after();
  }

  @Override
  public void clearText() {
    myFragments.clear();

    after();
  }

  public Component toComponent() {
    return new Span(myFragments.toArray(Component[]::new));
  }

  protected void after() {
  }
}
