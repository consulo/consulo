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

import consulo.util.collection.ArrayUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class AbstractDiagramVisibilityManager implements DiagramVisibilityManager {
    private @Nullable VisibilityLevel myCurrent;

    @Override
    public @Nullable VisibilityLevel getCurrentVisibilityLevel() {
        if (myCurrent == null) {
            VisibilityLevel[] levels = getVisibilityLevels();
            if (levels.length > 0) {
                myCurrent = levels[levels.length - 1];
            }
        }
        return myCurrent;
    }

    @Override
    public void setCurrentVisibilityLevel(VisibilityLevel level) {
        if (ArrayUtil.indexOf(getVisibilityLevels(), level) >= 0) {
            myCurrent = level;
        }
    }
}
