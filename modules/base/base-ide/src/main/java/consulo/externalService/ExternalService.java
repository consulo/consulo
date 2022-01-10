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
package consulo.externalService;

import com.intellij.openapi.application.ApplicationManager;
import consulo.application.ApplicationProperties;
import consulo.localize.LocalizeValue;
import consulo.util.lang.ThreeState;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 04/09/2021
 * <p>
 * States:
 * - YES - send or get requests, with user authorize (not anonymous)
 * - UNSURE - send or get requests anonymous
 * - NO - disabled service
 */
public enum ExternalService {
  ERROR_REPORTING(LocalizeValue.localizeTODO("Error Reporting"), ThreeState.UNSURE, ThreeState.YES, ThreeState.UNSURE, ThreeState.NO),
  DEVELOPER_LIST(LocalizeValue.localizeTODO("Developer List (Error Reporting)"), ThreeState.NO, ThreeState.YES, ThreeState.NO),
  STATISTICS(LocalizeValue.localizeTODO("Statistics"), ThreeState.UNSURE, ThreeState.YES, ThreeState.UNSURE, ThreeState.NO) {
    @Nonnull
    @Override
    public ThreeState getDefaultState() {
      // disable fully statistics for sandbox/internal mode
      if (ApplicationProperties.isInSandbox() || ApplicationManager.getApplication().isInternal()) {
        return ThreeState.NO;
      }
      return super.getDefaultState();
    }
  },
  STORAGE(LocalizeValue.localizeTODO("Settings Synchronize"), ThreeState.NO, ThreeState.YES, ThreeState.NO);

  private final LocalizeValue myName;
  private final ThreeState myDefaultState;
  private final ThreeState[] myAllowedStates;

  ExternalService(LocalizeValue name, ThreeState defaultState, ThreeState... allowedStates) {
    myName = name;
    myDefaultState = defaultState;
    myAllowedStates = allowedStates;
  }

  public ThreeState[] getAllowedStates() {
    return myAllowedStates;
  }

  public LocalizeValue getName() {
    return myName;
  }

  @Nonnull
  public ThreeState getDefaultState() {
    return myDefaultState;
  }
}
