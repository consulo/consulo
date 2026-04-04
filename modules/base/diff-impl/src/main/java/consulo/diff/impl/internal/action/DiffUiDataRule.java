/*
 * Copyright 2013-2025 consulo.io
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
package consulo.diff.impl.internal.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.dataContext.UiDataRule;
import consulo.diff.DiffDataKeys;
import consulo.navigation.Navigatable;

@ExtensionImpl
public class DiffUiDataRule implements UiDataRule {
    @Override
    public void uiDataSnapshot(DataSink sink, DataSnapshot snapshot) {
        Navigatable navigatable = snapshot.get(DiffDataKeys.NAVIGATABLE);
        if (navigatable != null) {
            sink.set(DiffDataKeys.NAVIGATABLE_ARRAY, new Navigatable[]{navigatable});
        }
    }
}
