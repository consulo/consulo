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
  public static class Fragment {
    public String inlineHTML;

    @Override
    public String toString() {
      return inlineHTML;
    }
  }

  private List<Fragment> myFragments = new ArrayList<>();

  @Nonnull
  @Override
  public TextItemPresentation withIcon(@Nullable Image image) {

    after();
    return this;
  }

  @Override
  public void append(@Nonnull LocalizeValue text, @Nonnull TextAttribute textAttribute) {
    Fragment fragment = new Fragment();
    fragment.inlineHTML = text.get();
    myFragments.add(fragment);

    after();
  }

  @Override
  public void clearText() {
    myFragments.clear();

    after();
  }

  public String toHTML() {
    return "<span>" + StringUtil.join(myFragments, "") + "</span>";
  }

  protected void after() {
  }
}
