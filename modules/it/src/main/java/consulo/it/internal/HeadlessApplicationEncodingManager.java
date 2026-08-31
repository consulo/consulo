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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import consulo.virtualFileSystem.encoding.EncodingReference;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * The production {@code EncodingManagerImpl} runs on the {@link ComponentProfiles#PRODUCTION} profile only;
 * content loading during indexing needs the application encoding manager, so the headless application provides
 * a UTF-8-only replacement.
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessApplicationEncodingManager implements ApplicationEncodingManager {
    @Override
    public Collection<Charset> getFavorites() {
        return List.of(StandardCharsets.UTF_8);
    }

    @Override
    public boolean isNative2Ascii(VirtualFile virtualFile) {
        return false;
    }

    @Override
    public boolean isNative2AsciiForPropertiesFiles() {
        return false;
    }

    @Override
    public Charset getDefaultCharset() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public @Nullable Charset getEncoding(@Nullable VirtualFile virtualFile, boolean useParentDefaults) {
        return null;
    }

    @Override
    public void setEncoding(@Nullable VirtualFile virtualFileOrDir, @Nullable Charset charset) {
    }

    @Override
    public void setNative2AsciiForPropertiesFiles(VirtualFile virtualFile, boolean native2Ascii) {
    }

    @Override
    public String getDefaultCharsetName() {
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public void setDefaultCharsetName(String name) {
    }

    @Override
    public @Nullable Charset getDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile) {
        return null;
    }

    @Override
    public void setDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile, @Nullable Charset charset) {
    }

    @Override
    public Charset getDefaultConsoleEncoding() {
        return StandardCharsets.UTF_8;
    }

    @Override
    public void setDefaultConsoleEncodingReference(EncodingReference encodingReference) {
    }

    @Override
    public EncodingReference getDefaultConsoleEncodingReference() {
        return EncodingReference.DEFAULT;
    }
}
