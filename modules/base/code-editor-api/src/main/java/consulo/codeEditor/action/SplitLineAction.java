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
package consulo.codeEditor.action;

import consulo.dataContext.DataContext;
import consulo.util.dataholder.Key;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2025-07-27
 */
public final class SplitLineAction {
    public static Key<Boolean> SPLIT_LINE_KEY = Key.create("consulo.codeEditor.impl.internal.action.SplitLineAction");

    private SplitLineAction() {
    }

    public static boolean isSplitAction(DataContext context) {
        return Objects.equals(context.getData(SPLIT_LINE_KEY), Boolean.TRUE);
    }
}
