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

import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * What a screen reader is told a component is called.
 * <p/>
 * Only the name and the description are here. What kind of thing a component is - its role - is not something a
 * caller sets, because a component of the api already knows what it is and where it sits, and each frontend says that
 * in its own way; a role handed over from outside would have to be translated into three vocabularies which do not
 * line up, and would carry none of the structure the browser requires of one.
 *
 * @author VISTALL
 * @since 2026-08-24
 */
public interface HasAccessibility {
    @RequiredUIAccess
    void setAccessibleName(LocalizeValue name);

    @RequiredUIAccess
    void setAccessibleDescription(LocalizeValue description);
}
