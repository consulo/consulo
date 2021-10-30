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
package consulo.ui.layout;

import consulo.ui.ComponentOptions;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 30/10/2021
 */
public final class ScrollableLayoutOptions extends ComponentOptions {
  public static class Builder extends ComponentOptionsBuilder<Builder> {
    private ScrollPolicy myHorizontalScrollPolicy = ScrollPolicy.IF_NEED;
    private ScrollPolicy myVerticalScrollPolicy = ScrollPolicy.IF_NEED;

    @Nonnull
    public Builder horizontalScrollPolicy(@Nonnull ScrollPolicy horizontalScrollPolicy) {
      myHorizontalScrollPolicy = horizontalScrollPolicy;
      return this;
    }

    @Nonnull
    public Builder verticalScrollPolicy(@Nonnull ScrollPolicy verticalScrollPolicy) {
      myVerticalScrollPolicy = verticalScrollPolicy;
      return this;
    }

    @Nonnull
    @Override
    public ScrollableLayoutOptions build() {
      return new ScrollableLayoutOptions(myBackgroundPaint, myHorizontalScrollPolicy, myVerticalScrollPolicy);
    }
  }

  public static enum ScrollPolicy {
    ALWAYS,
    IF_NEED,
    NEVER
  }

  @Nonnull
  public static Builder builder() {
    return new Builder();
  }

  private final ScrollPolicy myHorizontalScrollPolicy;
  private final ScrollPolicy myVerticalScrollPolicy;

  private ScrollableLayoutOptions(boolean backgroundPaint, @Nonnull ScrollPolicy horizontalScrollPolicy, @Nonnull ScrollPolicy verticalScrollPolicy) {
    super(backgroundPaint);
    myHorizontalScrollPolicy = horizontalScrollPolicy;
    myVerticalScrollPolicy = verticalScrollPolicy;
  }

  @Nonnull
  public ScrollPolicy getHorizontalScrollPolicy() {
    return myHorizontalScrollPolicy;
  }

  @Nonnull
  public ScrollPolicy getVerticalScrollPolicy() {
    return myVerticalScrollPolicy;
  }
}
