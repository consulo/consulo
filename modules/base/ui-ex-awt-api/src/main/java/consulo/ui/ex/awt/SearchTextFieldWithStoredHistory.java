/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.application.ApplicationPropertiesComponent;
import consulo.component.PropertiesComponent;
import consulo.util.lang.StringUtil;

import java.util.ArrayList;

/**
 * @author max
 */
public class SearchTextFieldWithStoredHistory extends SearchTextField {
  private final String myPropertyName;

  public SearchTextFieldWithStoredHistory(final String propertyName) {
    myPropertyName = propertyName;
    reset();
  }

  @Override
  public void addCurrentTextToHistory() {
    super.addCurrentTextToHistory();
    ApplicationPropertiesComponent.getInstance().setValue(myPropertyName, StringUtil.join(getHistory(), "\n"));
  }

  public void reset() {
    final PropertiesComponent propertiesComponent = ApplicationPropertiesComponent.getInstance();
    final String history = propertiesComponent.getValue(myPropertyName);
    if (history != null) {
      final String[] items = history.split("\n");
      ArrayList<String> result = new ArrayList<>();
      for (String item : items) {
        if (item != null && item.length() > 0) {
          result.add(item);
        }
      }
      setHistory(result);
    }
    setSelectedItem("");
  }
}