/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.impl;

import consulo.ide.eap.EarlyAccessProgramDescriptor;
import consulo.ide.eap.EarlyAccessProgramManager;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 01-Nov-17
 */
public class ModalityPerProjectEAPDescriptor extends EarlyAccessProgramDescriptor {
  public static boolean is() {
    return EarlyAccessProgramManager.is(ModalityPerProjectEAPDescriptor.class);
  }

  @Nonnull
  @Override
  public String getName() {
    return "Modality per project";
  }

  @Override
  public boolean isRestartRequired() {
    return true;
  }
}
