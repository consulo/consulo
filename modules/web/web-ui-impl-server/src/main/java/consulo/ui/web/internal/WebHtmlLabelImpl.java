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
package consulo.ui.web.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.HtmlLabel;
import consulo.ui.LabelOptions;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public class WebHtmlLabelImpl extends WebLabelBase<WebHtmlLabelImpl.Vaadin> implements HtmlLabel {
  public static class Vaadin extends VaadinLabelComponentBase {

  }

  public WebHtmlLabelImpl(LocalizeValue text, LabelOptions options) {
    super(text, options);
  }

  @Nonnull
  @Override
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }
}
