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
package consulo.ui.internal;

import consulo.ui.HtmlLabel;

/**
 * @author VISTALL
 * @since 12-Jun-16
 */
public class DesktopHtmlLabelImpl extends DesktopLabelImpl implements HtmlLabel {
  public DesktopHtmlLabelImpl(String text) {
    super("<html><body>" + text + "</body></html>");
  }

  @Override
  public void dispose() {

  }
}
