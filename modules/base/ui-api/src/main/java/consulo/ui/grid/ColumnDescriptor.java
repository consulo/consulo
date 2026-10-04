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
package consulo.ui.grid;

import java.util.Collections;
import java.util.Set;

public interface ColumnDescriptor {
    // For some data sources this type is not reliable. For example, in CSV, the column type detected by the first N rows
    // and possible, for example, that the type of Double column will be Int or the type of String column will be Long.
    GridDataType getType();

    String getName();

    default Set<Attribute> getAttributes() {
        return Collections.emptySet();
    }

    enum Attribute {
        ZERO_PADDING,
        ROW_ID,
        HIDDEN,
        INDEX,
        VIRTUAL,
        MULTI_DIMENSIONAL_ARRAY,
        /**
         * Consulo: the name is made up from the position of the column - no header names it - so it does not tell the column
         * apart from the one which takes its position after a column is moved, inserted or deleted.
         */
        GENERATED_NAME
    }
}
