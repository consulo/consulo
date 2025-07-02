/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

/**
 * created at Jan 3, 2002
 *
 * @author Jeka
 */
package consulo.compiler.setting;

import consulo.disposer.Disposable;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nullable;

public class ExcludeEntryDescription implements Disposable {
    private boolean myIsFile;
    private boolean myIncludeSubdirectories;
    private VirtualFilePointer myFilePointer;
    private final Disposable myParentDisposable;

    public ExcludeEntryDescription(String url, boolean includeSubdirectories, boolean isFile, Disposable parent) {
        myParentDisposable = parent;
        myFilePointer = VirtualFilePointerManager.getInstance().create(url, parent, null);
        myIncludeSubdirectories = includeSubdirectories;
        myIsFile = isFile;
    }

    public ExcludeEntryDescription(VirtualFile virtualFile, boolean includeSubdirectories, boolean isFile, Disposable parent) {
        this(virtualFile.getUrl(), includeSubdirectories, isFile, parent);
    }

    public ExcludeEntryDescription copy(Disposable parent) {
        return new ExcludeEntryDescription(getUrl(), myIncludeSubdirectories, myIsFile, parent);
    }

    public void setPresentableUrl(String newUrl) {
        myFilePointer = VirtualFilePointerManager.getInstance()
            .create(VirtualFileUtil.pathToUrl(FileUtil.toSystemIndependentName(newUrl)), myParentDisposable, null);
        final VirtualFile file = getVirtualFile();
        if (file != null) {
            myIsFile = !file.isDirectory();
        }
    }

    public boolean isFile() {
        return myIsFile;
    }

    public String getUrl() {
        return myFilePointer.getUrl();
    }

    public String getPresentableUrl() {
        return myFilePointer.getPresentableUrl();
    }

    public boolean isIncludeSubdirectories() {
        return myIncludeSubdirectories;
    }

    public void setIncludeSubdirectories(boolean includeSubdirectories) {
        myIncludeSubdirectories = includeSubdirectories;
    }

    @Nullable
    public VirtualFile getVirtualFile() {
        return myFilePointer.getFile();
    }

    public boolean isValid() {
        return myFilePointer.isValid();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ExcludeEntryDescription)) {
            return false;
        }
        ExcludeEntryDescription entryDescription = (ExcludeEntryDescription) obj;
        if (entryDescription.myIsFile != myIsFile) {
            return false;
        }
        if (entryDescription.myIncludeSubdirectories != myIncludeSubdirectories) {
            return false;
        }
        return Comparing.equal(entryDescription.getUrl(), getUrl());
    }

    public int hashCode() {
        int result = (myIsFile ? 1 : 0);
        result = 31 * result + (myIncludeSubdirectories ? 1 : 0);
        result = 31 * result + getUrl().hashCode();
        return result;
    }

    public void dispose() {
    }
}
