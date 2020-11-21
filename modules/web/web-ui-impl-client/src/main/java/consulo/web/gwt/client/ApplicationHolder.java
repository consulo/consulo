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
package consulo.web.gwt.client;

import consulo.web.gwt.shared.ui.state.ApplicationState;
import consulo.web.gwt.shared.ui.state.RGBColorShared;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public class ApplicationHolder {
  public static final ApplicationHolder INSTANCE = new ApplicationHolder();

  private ApplicationState myApplicationState = new ApplicationState();

  public void setApplicationState(ApplicationState state) {
    myApplicationState = state;
  }

  public RGBColorShared getComponentColor(@Nonnull String colorKey) {
    RGBColorShared shared = myApplicationState.myComponentColors.get(colorKey);
    if (shared != null) {
      return shared;
    }
    return new RGBColorShared();
  }

  public RGBColorShared getSchemeColor(@Nonnull String colorKey) {
    RGBColorShared shared = myApplicationState.mySchemeColors.get(colorKey);
    if(shared != null) {
      return shared;
    }
    return new RGBColorShared();
  }
}
