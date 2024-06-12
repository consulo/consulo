/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.content.bundle;

import consulo.application.AllIcons;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Used as a plug for all SDKs which type cannot be determined (for example, plugin that registered a custom type has been deinstalled)
 * @author Eugene Zhuravlev
 *         Date: Dec 11, 2004
 */
public class UnknownSdkType extends SdkType{
  public static UnknownSdkType getInstance(String typeName) {
    return ourTypeNameToInstanceMap.computeIfAbsent(typeName, UnknownSdkType::new);
  }

  private static final Map<String, UnknownSdkType> ourTypeNameToInstanceMap = new ConcurrentHashMap<>();

  /**
   * @param typeName the name of the SDK type that this SDK serves as a plug for
   */
  private UnknownSdkType(String typeName) {
    super(typeName);
  }

  @Override
  public boolean isValidSdkHome(String path) {
    return false;
  }

  @Override
  public String getVersionString(String sdkHome) {
    return "";
  }

  @Override
  public String suggestSdkName(String currentSdkName, String sdkHome) {
    return currentSdkName;
  }
  @Nonnull
  @Override
  public String getPresentableName() {
    return ProjectLocalize.sdkUnknownName().get();
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Actions.Help;
  }
}
