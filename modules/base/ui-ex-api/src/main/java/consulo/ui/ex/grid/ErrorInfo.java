// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.ui.ex.grid;

import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.ui.grid.ThrowableInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * An error which offers fixes. A data source reports it where {@code consulo.ui.grid.GridDataHookUp.RequestListener}
 * takes a plain {@link ThrowableInfo}, since the data context its fixes need is not visible from {@code consulo.ui.api}.
 */
public interface ErrorInfo extends ThrowableInfo {
    List<Fix> getFixes();


    interface Fix {
        LocalizeValue getName();

        default @Nullable Integer getMnemonic() {
            return null;
        }

        default boolean isSilent() {
            return true;
        }

        void apply(DataContext dataContext);
    }
}
