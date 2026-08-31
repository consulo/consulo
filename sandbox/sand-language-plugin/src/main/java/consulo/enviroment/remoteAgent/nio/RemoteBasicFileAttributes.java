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
package consulo.enviroment.remoteAgent.nio;

import consulo.enviroment.remoteAgent.protocol.FileInfo;

import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/**
 * @author VISTALL
 * @since 2026-03-17
 */
public class RemoteBasicFileAttributes implements BasicFileAttributes {
    private final FileInfo myFileInfo;

    public RemoteBasicFileAttributes(FileInfo fileInfo) {
        myFileInfo = fileInfo;
    }

    @Override
    public FileTime lastModifiedTime() {
        if (myFileInfo.isSetLastModified()) {
            return FileTime.fromMillis(myFileInfo.getLastModified());
        }
        return FileTime.fromMillis(0);
    }

    @Override
    public FileTime lastAccessTime() {
        return lastModifiedTime();
    }

    @Override
    public FileTime creationTime() {
        return lastModifiedTime();
    }

    @Override
    public boolean isRegularFile() {
        return !myFileInfo.isDirectory() && !myFileInfo.isSymlink();
    }

    @Override
    public boolean isDirectory() {
        return myFileInfo.isDirectory();
    }

    @Override
    public boolean isSymbolicLink() {
        return myFileInfo.isSymlink();
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        return myFileInfo.getSize();
    }

    @Override
    public Object fileKey() {
        return myFileInfo.getPath();
    }
}
