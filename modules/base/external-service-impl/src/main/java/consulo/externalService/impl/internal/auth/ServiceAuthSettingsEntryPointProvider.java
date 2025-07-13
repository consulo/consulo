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
package consulo.externalService.impl.internal.auth;

import consulo.annotation.component.ExtensionImpl;
import consulo.externalService.ExternalServiceConfiguration;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.internal.SettingsEntryPointActionProvider;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * @author VISTALL
 * @since 18/08/2021
 */
@ExtensionImpl
public class ServiceAuthSettingsEntryPointProvider implements SettingsEntryPointActionProvider {
  private LoginAction myLoginAction;

  private final Provider<ExternalServiceConfiguration> myExternalServiceConfigurationProvider;

  @Inject
  public ServiceAuthSettingsEntryPointProvider(Provider<ExternalServiceConfiguration> externalServiceConfigurationProvider) {
    myExternalServiceConfigurationProvider = externalServiceConfigurationProvider;
  }

  @Nonnull
  @Override
  public AnAction getUpdateActionOrGroup() {
    if (myLoginAction == null) {
      myLoginAction = new LoginAction(myExternalServiceConfigurationProvider);
    }
    return ActionGroup.of(myLoginAction, AnSeparator.create());
  }
}
