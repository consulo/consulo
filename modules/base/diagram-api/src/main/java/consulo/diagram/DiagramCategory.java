/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package consulo.diagram;

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author Konstantin Bulenkov
 */
public class DiagramCategory {
    public static final DiagramCategory[] EMPTY_ARRAY = {};

    private final String myName;
    private final @Nullable Image myIcon;

    public DiagramCategory(String name, @Nullable Image icon) {
        myName = name;
        myIcon = icon;
    }

    public String getName() {
        return myName;
    }

    public @Nullable Image getIcon() {
        return myIcon;
    }

    @Override
    public String toString() {
        return myName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(myName, ((DiagramCategory) o).myName);
    }

    @Override
    public int hashCode() {
        return myName.hashCode();
    }
}
