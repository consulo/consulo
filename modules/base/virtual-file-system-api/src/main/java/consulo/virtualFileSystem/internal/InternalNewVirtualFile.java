/*
 * Copyright 2013-2025 consulo.io
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
package consulo.virtualFileSystem.internal;

import consulo.virtualFileSystem.NewVirtualFile;
import consulo.virtualFileSystem.VirtualFile;

import java.util.Collection;

/**
 * @author VISTALL
 * @since 2025-06-20
 */
public abstract class InternalNewVirtualFile extends NewVirtualFile {
    public abstract void setFileIndexed(boolean value);

    public abstract boolean isFileIndexed();

    public abstract void markDirtyInternal();

    public abstract int getNameId();

    @Override
    public abstract Collection<VirtualFile> getCachedChildren();
}
