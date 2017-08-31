/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.gwt.shared;

import consulo.web.gwt.shared.UIComponent;
import consulo.web.gwt.shared.UIServerEventType;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class UIServerEvent implements Serializable {
  private UIServerEventType myType;
  private String myId;
  private LinkedList<UIComponent> myComponents;

  public void setType(UIServerEventType type) {
    myType = type;
  }

  public UIServerEventType getType() {
    return myType;
  }

  public void setSessionId(String id) {
    myId = id;
  }

  public String getSessionId() {
    return myId;
  }

  public List<UIComponent> getComponents() {
    return myComponents;
  }

  public void setComponents(List<UIComponent> components) {
    myComponents = new LinkedList<>(components);
  }
}
