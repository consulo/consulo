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
package consulo.ui;

import consulo.localize.LocalizeValue;
import consulo.ui.internal.UIInternal;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public interface HtmlLabel extends Label {
  @Nonnull
  static HtmlLabel create(@Nonnull LocalizeValue html) {
    return create(html, LabelOptions.builder().build());
  }

  @Nonnull
  static HtmlLabel create(@Nonnull LocalizeValue html, @Nonnull LabelOptions labelOptions) {
    return UIInternal.get()._Components_htmlLabel(html, labelOptions);
  }
}
