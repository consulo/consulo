/*
 * Copyright 2013-2020 consulo.io
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
package consulo.web.internal.ui.image;

import consulo.ui.impl.image.ImageReference;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-10-03
 */
public class WebImageReference implements ImageReference {
  private final byte[] myX1Data;
  private final byte[] myX2Data;
  private final boolean myIsSVG;

  public WebImageReference(byte[] x1Data, byte[] x2Data, boolean isSVG) {
    myX1Data = x1Data;
    myX2Data = x2Data;
    myIsSVG = isSVG;
  }

  public boolean isSVG() {
    return myIsSVG;
  }

  @Nonnull
  public byte[] getData() {
    if (myX2Data != null) {
      return myX2Data;
    }

    return myX1Data;
  }
}
