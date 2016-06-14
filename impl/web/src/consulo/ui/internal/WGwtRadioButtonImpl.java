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

import com.intellij.openapi.util.Comparing;
import consulo.ui.RadioButton;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class WGwtRadioButtonImpl extends WGwtBooleanValueComponentImpl implements RadioButton {
  private String myText;

  public WGwtRadioButtonImpl(boolean selected, String text) {
    super(selected);
    myText = text;
  }

  @Override
  @NotNull
  public String getText() {
    return myText;
  }

  @Override
  public void setText(@NotNull final String text) {
    if (Comparing.equal(myText, text)) {
      return;
    }

    myText = text;

    markAsChanged();
  }

  @Override
  protected void getState(Map<String, String> map) {
    super.getState(map);

    map.put("text", myText);
  }
}
