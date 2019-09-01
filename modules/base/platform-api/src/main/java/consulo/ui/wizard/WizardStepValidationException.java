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
package consulo.ui.wizard;

import consulo.ui.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-08-20
 */
public class WizardStepValidationException extends Exception {
  private final Object myComponent;
  private final String myMessage;

  public WizardStepValidationException(@Nonnull String message) {
    myComponent = null;
    myMessage = message;
  }

  public WizardStepValidationException(@Nonnull Component component, @Nonnull String message) {
    myComponent = component;
    myMessage = message;
  }

  public WizardStepValidationException(@Nonnull java.awt.Component component, @Nonnull String message) {
    myComponent = component;
    myMessage = message;
  }

  @Nullable
  public Object getComponent() {
    return myComponent;
  }

  @Override
  public String getMessage() {
    return myMessage;
  }
}
