/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.content.bundle.SdkModel;
import consulo.content.bundle.SdkModelFactory;
import consulo.ide.setting.ShowSettingsUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 14-Apr-22
 */
@Singleton
@ServiceImpl
public class IdeSdkModelFactory implements SdkModelFactory {
  private final ShowSettingsUtil myShowSettingsUtil;

  @Inject
  public IdeSdkModelFactory(ShowSettingsUtil showSettingsUtil) {
    myShowSettingsUtil = showSettingsUtil;
  }

  @Nonnull
  @Override
  public SdkModel getOrCreateModel() {
    return myShowSettingsUtil.getSdksModel();
  }
}
