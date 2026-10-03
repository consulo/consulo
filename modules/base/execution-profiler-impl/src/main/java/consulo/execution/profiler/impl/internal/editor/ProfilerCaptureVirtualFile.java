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
package consulo.execution.profiler.impl.internal.editor;

import consulo.execution.profiler.impl.internal.session.ProfilerCapture;
import consulo.virtualFileSystem.light.LightVirtualFileBase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerCaptureVirtualFile extends LightVirtualFileBase {
    private final ProfilerCapture myCapture;

    public ProfilerCaptureVirtualFile(ProfilerCapture capture) {
        super(capture.getFileName(), ProfilerFileType.CAPTURE, System.currentTimeMillis());
        myCapture = capture;
        setWritable(false);
    }

    public ProfilerCapture getCapture() {
        return myCapture;
    }

    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        throw new IOException("A profiler capture can't be written: " + getName());
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
