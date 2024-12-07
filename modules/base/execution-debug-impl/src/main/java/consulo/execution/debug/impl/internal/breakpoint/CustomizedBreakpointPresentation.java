/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.breakpoint;

import consulo.ui.image.Image;

import jakarta.annotation.Nullable;

/**
 * @author nik
*/
public class CustomizedBreakpointPresentation {
  private Image myIcon;
  private String myErrorMessage;

  public void setIcon(final Image icon) {
    myIcon = icon;
  }

  public void setErrorMessage(final String errorMessage) {
    myErrorMessage = errorMessage;
  }

  @Nullable
  public Image getIcon() {
    return myIcon;
  }

  public String getErrorMessage() {
    return myErrorMessage;
  }
}
