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
package consulo.auth;

import com.intellij.ide.actions.SettingsEntryPointAction;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import consulo.auth.action.LoginAction;
import consulo.ide.eap.EarlyAccessProgramManager;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 18/08/2021
 */
public class ServiceAuthSettingsEntryPointProvider implements SettingsEntryPointAction.ActionProvider {
  private LoginAction myLoginAction;

  @Nonnull
  @Override
  public Collection<AnAction> getUpdateActions(@Nonnull DataContext context) {
    if (EarlyAccessProgramManager.is(ServiceAuthEarlyAccessProgramDescriptor.class)) {
      if (myLoginAction == null) {
        myLoginAction = new LoginAction();
      }
      return List.of(myLoginAction);
    }
    return List.of();
  }
}
