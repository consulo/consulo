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
package consulo.externalSystem.rt.model;

/**
 * @author Vladislav.Soroka
 * @since 7/22/2014
 */
public class DefaultExternalFilter implements ExternalFilter {
  private static final long serialVersionUID = 1L;

  private String myFilterType;
  private String myPropertiesAsJsonMap;

  public DefaultExternalFilter() {
    myPropertiesAsJsonMap = "";
    myFilterType = "";
  }


  public DefaultExternalFilter(ExternalFilter filter) {
    myPropertiesAsJsonMap = filter.getPropertiesAsJsonMap();
    myFilterType = filter.getFilterType();
  }

  @Override
  public String getFilterType() {
    return myFilterType;
  }

  public void setFilterType(String filterType) {
    myFilterType = filterType;
  }

  @Override
  public String getPropertiesAsJsonMap() {
    return myPropertiesAsJsonMap;
  }

  public void setPropertiesAsJsonMap(String propertiesAsJsonMap) {
    myPropertiesAsJsonMap = propertiesAsJsonMap;
  }
}
