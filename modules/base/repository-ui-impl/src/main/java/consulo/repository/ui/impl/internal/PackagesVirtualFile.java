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
package consulo.repository.ui.impl.internal;

import consulo.virtualFileSystem.VirtualFileSystem;
import consulo.virtualFileSystem.light.LightVirtualFileBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * The packages of a target of a provider: the path is {@code <provider id>/<encoded target>}, so the editor of it is restored with the project.
 */
public class PackagesVirtualFile extends LightVirtualFileBase {
    private final String myProviderId;
    private final String myTarget;
    private final VirtualFileSystem myFileSystem;

    public PackagesVirtualFile(String providerId, String target, String name, VirtualFileSystem fileSystem) {
        super(name, PackagesFileType.INSTANCE, 0);
        myProviderId = providerId;
        myTarget = target;
        myFileSystem = fileSystem;
        setWritable(false);
    }

    public static String buildPath(String providerId, String target) {
        return providerId + "/" + URLEncoder.encode(target, StandardCharsets.UTF_8);
    }

    public String getProviderId() {
        return myProviderId;
    }

    public String getTarget() {
        return myTarget;
    }

    @Override
    public String getPath() {
        return buildPath(myProviderId, myTarget);
    }

    @Override
    public VirtualFileSystem getFileSystem() {
        return myFileSystem;
    }

    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] contentsToByteArray() throws IOException {
        return new byte[0];
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(new byte[0]);
    }
}
