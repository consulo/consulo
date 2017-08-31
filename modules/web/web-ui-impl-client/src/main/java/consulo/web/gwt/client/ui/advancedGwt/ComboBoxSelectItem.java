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
package consulo.web.gwt.client.ui.advancedGwt;

import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.SimplePanel;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class ComboBoxSelectItem extends SimplePanel implements HasFocusHandlers, HasBlurHandlers, HasClickHandlers, HasAllKeyHandlers {
  public ComboBoxSelectItem() {
    sinkEvents(Event.ONCLICK | Event.ONBLUR | Event.ONFOCUS | Event.KEYEVENTS);
  }

  @Override
  public HandlerRegistration addBlurHandler(BlurHandler handler) {
    return addDomHandler(handler, BlurEvent.getType());
  }

  @Override
  public HandlerRegistration addFocusHandler(FocusHandler handler) {
    return addDomHandler(handler, FocusEvent.getType());
  }

  @Override
  public HandlerRegistration addClickHandler(ClickHandler handler) {
    return addDomHandler(handler, ClickEvent.getType());
  }

  @Override
  public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
    return addDomHandler(handler, KeyDownEvent.getType());
  }

  @Override
  public HandlerRegistration addKeyPressHandler(KeyPressHandler handler) {
    return addDomHandler(handler, KeyPressEvent.getType());
  }

  @Override
  public HandlerRegistration addKeyUpHandler(KeyUpHandler handler) {
    return addDomHandler(handler, KeyUpEvent.getType());
  }
}
