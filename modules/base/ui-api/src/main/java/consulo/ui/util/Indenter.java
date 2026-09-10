/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ui.util;

import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.WrappedLayout;

/**
 * @author VISTALL
 * @since 2022-08-01
 */
public class Indenter {
    @RequiredUIAccess
    public static Component indent(Component target) {
        WrappedLayout layout = WrappedLayout.create().set(target);
        layout.paddingBuilder().leftSet(Space.X_LARGE).apply();
        return layout;
    }
}
