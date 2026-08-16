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
package consulo.ui;

import consulo.ui.internal.UIInternal;

/**
 * A line drawn between the components standing beside it. Unlike {@link MenuSeparator} it is a component of its
 * own, so it can be put into any layout.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public interface Separator extends Component {
    static Separator create(SeparatorStyle style) {
        return UIInternal.get()._Separator_create(style);
    }

    static Separator horizontal() {
        return create(SeparatorStyle.HORIZONTAL);
    }

    static Separator vertical() {
        return create(SeparatorStyle.VERTICAL);
    }

    SeparatorStyle getSeparatorStyle();
}
