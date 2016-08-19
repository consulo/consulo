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
package consulo.web.gwt.shared.transport;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class GwtHighlightInfo implements IsSerializable {
  private GwtTextAttributes myTextAttributes;
  private GwtTextRange myTextRange;
  private String myTooltip;
  private int mySeverity;

  public GwtHighlightInfo() {
  }

  public GwtHighlightInfo(GwtTextAttributes textAttributes, GwtTextRange textRange, int severity) {
    myTextAttributes = textAttributes;
    myTextRange = textRange;
    mySeverity = severity;
  }

  public boolean isEmpty() {
    return myTextAttributes == null && myTooltip == null;
  }

  public int getSeverity() {
    return mySeverity;
  }

  public String getTooltip() {
    return myTooltip;
  }

  public void setTooltip(String tooltip) {
    myTooltip = tooltip;
  }

  public GwtTextAttributes getTextAttributes() {
    return myTextAttributes;
  }

  public GwtTextRange getTextRange() {
    return myTextRange;
  }
}
