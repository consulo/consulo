/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.content.scope;

import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

public abstract class BaseSearchScope implements SearchScope {
    private static int hashCodeCounter = 0;

    @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod"})
    private final int myHashCode = hashCodeCounter++;

    /**
     * Overridden for performance reason. Object.hashCode() is native method and becomes a bottleneck when called often.
     *
     * @return hashCode value semantically identical to one from Object but not native
     */
    @Override
    public int hashCode() {
        return myHashCode;
    }

    @Override
    public String getDisplayName() {
        return "<unknown scope>";
    }

    @Override
    public @Nullable Image getIcon() {
        return null;
    }

    @Override
    public abstract SearchScope intersectWith(SearchScope scope2);

    @Override
    public abstract SearchScope union(SearchScope scope);

    @Override
    public abstract boolean contains(VirtualFile file);
}
