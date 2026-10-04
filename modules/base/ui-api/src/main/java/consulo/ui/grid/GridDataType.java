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

/**
 * Type of a grid column - its {@link GridTypeKind} plus the name the data source gives it. It takes the place of a
 * {@code java.sql.Types} code, so sources which are not JDBC can describe their columns too.
 *
 * @since 2026-10-03
 */
public interface GridDataType {
    GridDataType UNKNOWN = of(GridTypeKind.OTHER, "");

    static GridDataType of(GridTypeKind kind, String name) {
        return new GridDataTypeImpl(kind, name);
    }

    GridTypeKind getKind();

    /**
     * Native type name shown to the user, for example {@code varchar(255)}, {@code ObjectId} or {@code INTEGER}.
     * Empty when the data source has none.
     */
    String getName();
}
