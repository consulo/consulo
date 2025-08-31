/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.application;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.component.messagebus.MessageBus;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class PowerSaveMode {
  private static final String POWER_SAVE_MODE = "power.save.mode";
  private boolean myEnabled = ApplicationPropertiesComponent.getInstance().getBoolean(POWER_SAVE_MODE, false);
  private final MessageBus myBus;

  @Inject
  public PowerSaveMode(Application application) {
    myBus = application.getMessageBus();
  }

  public static boolean isEnabled() {
    return Application.get().getInstance(PowerSaveMode.class).myEnabled;
  }

  public static void setEnabled(boolean value) {
    PowerSaveMode instance = Application.get().getInstance(PowerSaveMode.class);
    if (instance.myEnabled != value) {
      instance.myEnabled = value;
      instance.myBus.syncPublisher(PowerSaveModeListener.class).powerSaveStateChanged();

      ApplicationPropertiesComponent.getInstance().setValue(POWER_SAVE_MODE, String.valueOf(value));
    }
  }
}
