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
package consulo.ide.impl.idea.openapi.roots.impl.libraries;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.content.library.LibraryType;
import jakarta.inject.Singleton;

/**
 * @author nik
 */
@Singleton
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
@ServiceImpl
public class LibraryKindLoader {
    public LibraryKindLoader() {
        //todo[nik] this is temporary workaround for IDEA-98118: we need to initialize all library types
        // to ensure that their kinds are created and registered in LibraryKind.ourAllKinds
        // In order to properly fix the problem we should extract all UI-related methods
        // from LibraryType to a separate class and move LibraryType to projectModel-impl module
        LibraryType.EP_NAME.getExtensions();
    }
}
