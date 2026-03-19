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

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2021-05-03
 */
public final class WindowOptions extends ComponentOptions {
  public static final class Builder {
    private boolean myClosable = true;
    private boolean myResizable = true;
    private @Nullable Window myOwner = null;

    private Builder() {
    }

    public Builder owner(@Nullable Window owner) {
      myOwner = owner;
      return this;
    }

    public Builder disableResize() {
      myResizable = false;
      return this;
    }

    public Builder disableClose() {
      myClosable = false;
      return this;
    }

    public WindowOptions build() {
      return new WindowOptions(myOwner, myClosable, myResizable);
    }
  }

  public static WindowOptions.Builder builder() {
    return new Builder();
  }

  private final @Nullable Window myOwner;
  private final boolean myClosable;
  private final boolean myResizable;

  private WindowOptions(@Nullable Window owner, boolean closable, boolean resizable) {
    super(true);
    
    myOwner = owner;
    myClosable = closable;
    myResizable = resizable;
  }

  public @Nullable Window getOwner() {
    return myOwner;
  }

  public boolean isClosable() {
    return myClosable;
  }

  public boolean isResizable() {
    return myResizable;
  }
}
