/*
 * Copyright 2013-2014 must-be.org
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
package org.mustbe.consulo.sandLanguage.ide.bundle;

import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.types.BinariesOrderRootType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12.05.14
 */
public class SandBundleType extends SdkType {
  public SandBundleType() {
    super("SAND_BUNDLE");
  }

  @Nullable
  @Override
  public String suggestHomePath() {
    return null;
  }

  @Override
  public boolean isValidSdkHome(String path) {
    return true;
  }

  @Nullable
  @Override
  public String getVersionString(String sdkHome) {
    return "1";
  }

  @Override
  public boolean isRootTypeApplicable(OrderRootType type) {
    return type == BinariesOrderRootType.getInstance();
  }

  @Override
  public String suggestSdkName(String currentSdkName, String sdkHome) {
    return sdkHome;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Sand Bundle";
  }
}
