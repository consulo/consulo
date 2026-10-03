/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal.ui;


import consulo.ui.TextBoxWithHistory;
import consulo.util.lang.StringUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public class HeadlessTextBoxWithHistory extends HeadlessTextBox implements TextBoxWithHistory {
    private List<String> myHistory = List.of();

    public HeadlessTextBoxWithHistory(String text) {
        super(text);
    }

    @Override
    public TextBoxWithHistory setHistory(List<String> history) {
        List<String> items = new ArrayList<>(history.size());
        for (String item : history) {
            items.add(StringUtil.notNullize(item));
        }
        myHistory = List.copyOf(items);
        return this;
    }

    public List<String> getHistory() {
        return myHistory;
    }
}
