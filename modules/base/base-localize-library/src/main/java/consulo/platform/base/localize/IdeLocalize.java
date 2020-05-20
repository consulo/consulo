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
package consulo.platform.base.localize;

import consulo.localize.*;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-04-11
 */
public class IdeLocalize {
  private static final LocalizeKey checkbox_synchronize_files_on_frame_activation = LocalizeKey.of("consulo.platform.base.IdeLocalize", "checkbox.synchronize.files.on.frame.activation");

  @Nonnull
  public static LocalizeValue checkboxSynchronizeFilesOnFrameActivation() {
    return checkbox_synchronize_files_on_frame_activation.getValue();
  }
}
