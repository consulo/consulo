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
package consulo.desktop.swt.platform;

import consulo.platform.PlatformBase;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public class DesktopSwtPlatformImpl extends PlatformBase {
  @Override
  public boolean isDesktop() {
    return false;
  }

  @Override
  public boolean isWebService() {
    return false;
  }
}
