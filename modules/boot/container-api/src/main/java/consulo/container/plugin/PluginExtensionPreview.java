/*
 * Copyright 2013-2023 consulo.io
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
package consulo.container.plugin;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 25/05/2023
 */
public final class PluginExtensionPreview {
  private final PluginId apiPluginId;
  private final String apiClassName;
  private final String implId;

  public PluginExtensionPreview(Class<?> apiClass, String implId) {
    this.apiPluginId = PluginManager.getPluginId(apiClass);
    this.apiClassName = apiClass.getName();
    this.implId = implId;
  }

  public PluginExtensionPreview(PluginId apiPluginId, String apiClassName, String implId) {
    this.apiPluginId = apiPluginId;
    this.apiClassName = apiClassName;
    this.implId = implId;
  }

  public PluginId getApiPluginId() {
    return apiPluginId;
  }

  public String getApiClassName() {
    return apiClassName;
  }

  public String getImplId() {
    return implId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PluginExtensionPreview that = (PluginExtensionPreview)o;
    return Objects.equals(apiPluginId, that.apiPluginId) &&
      Objects.equals(apiClassName, that.apiClassName) &&
      Objects.equals(implId, that.implId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiPluginId, apiClassName, implId);
  }

  @Override
  public String toString() {
    return "PluginExtensionPreview{" +
      "apiPluginId=" + apiPluginId +
      ", apiClassName='" + apiClassName + '\'' +
      ", implId='" + implId + '\'' +
      '}';
  }
}
