/*
 * Copyright 2013-2021 consulo.io
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

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 03/05/2021
 */
public final class LabelOptions extends ComponentOptions {
  public static final class Builder extends ComponentOptionsBuilder<Builder> {
    private HorizontalAlignment myHorizontalAlignment = HorizontalAlignment.LEFT;

    @Nonnull
    public Builder horizontalAlignment(@Nonnull HorizontalAlignment alignment) {
      myHorizontalAlignment = alignment;
      return this;
    }

    @Nonnull
    @Override
    public LabelOptions build() {
      return new LabelOptions(myBackgroundPaint, myHorizontalAlignment);
    }
  }

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  private final HorizontalAlignment myHorizontalAlignment;

  private LabelOptions(boolean backgroundPaint, @Nonnull HorizontalAlignment horizontalAlignment) {
    super(backgroundPaint);
    myHorizontalAlignment = horizontalAlignment;
  }

  @Nonnull
  public HorizontalAlignment getHorizontalAlignment() {
    return myHorizontalAlignment;
  }
}
