/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.gwt.shared.state;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @author VISTALL
 * @since 09-Sep-17
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UIComponentState {
  private long myId;
  private String myType;
  private long myEventBits;

  public long getId() {
    return myId;
  }

  public void setId(long id) {
    myId = id;
  }

  public String getType() {
    return myType;
  }

  public void setType(String type) {
    myType = type;
  }

  public long getEventBits() {
    return myEventBits;
  }

  public void setEventBits(long eventBits) {
    this.myEventBits = eventBits;
  }
}
