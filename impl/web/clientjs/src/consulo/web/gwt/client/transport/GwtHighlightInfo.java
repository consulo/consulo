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
package consulo.web.gwt.client.transport;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class GwtHighlightInfo implements IsSerializable {
  private GwtColor myForeground;
  private boolean myBold;
  private boolean myItalic;
  private GwtTextRange myTextRange;

  public GwtHighlightInfo() {
  }

  public GwtHighlightInfo(GwtColor foreground, boolean bold, boolean italic, GwtTextRange textRange) {
    myForeground = foreground;
    myBold = bold;
    myItalic = italic;
    myTextRange = textRange;
  }

  public GwtColor getForeground() {
    return myForeground;
  }

  public boolean isBold() {
    return myBold;
  }

  public boolean isItalic() {
    return myItalic;
  }

  public GwtTextRange getTextRange() {
    return myTextRange;
  }
}
