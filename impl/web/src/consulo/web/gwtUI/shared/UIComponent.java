/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwtUI.shared;

import java.util.List;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public interface UIComponent extends UIVariablesOwner {
  interface Child extends UIVariablesOwner {
    UIComponent getComponent();

    void setComponent(UIComponent component);
  }

  long getId();

  void setId(long id);

  String getType();

  void setType(String type);

  List<Child> getChildren();

  void setChildren(List<Child> children);
}
