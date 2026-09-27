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
import consulo.component.util.ModificationTracker;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingManager;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

/**
 * A project has no encoding configuration of its own in a headless run - {@link HeadlessApplicationEncodingManager}
 * already answers that there is no project manager - so this one holds no per-file mappings and hands every
 * question about defaults to the application. It exists because creating a file through the VFS asks the file's
 * project whether new UTF-8 files get a BOM.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessEncodingProjectManager implements EncodingProjectManager, ModificationTracker {
    private static EncodingManager application() {
        return EncodingManager.getInstance();
    }

    @Override
    public ModificationTracker getModificationTracker() {
        return this;
    }

    @Override
    public long getModificationCount() {
        return 0;
    }

    @Override
    public Map<? extends VirtualFile, ? extends Charset> getAllMappings() {
        return Map.of();
    }

    @Override
    public void setMapping(Map<? extends VirtualFile, ? extends Charset> mapping) {
    }

    @Override
    public Map<? extends VirtualFilePointer, ? extends Charset> getAllPointersMappings() {
        return Map.of();
    }

    @Override
    public void setPointerMapping(Map<? extends VirtualFilePointer, ? extends Charset> mapping) {
    }

    @Override
    public Collection<Charset> getFavorites() {
        return application().getFavorites();
    }

    @Override
    public boolean isNative2Ascii(VirtualFile virtualFile) {
        return application().isNative2Ascii(virtualFile);
    }

    @Override
    public boolean isNative2AsciiForPropertiesFiles() {
        return application().isNative2AsciiForPropertiesFiles();
    }

    @Override
    public void setNative2AsciiForPropertiesFiles(VirtualFile virtualFile, boolean native2Ascii) {
        application().setNative2AsciiForPropertiesFiles(virtualFile, native2Ascii);
    }

    @Override
    public String getDefaultCharsetName() {
        return application().getDefaultCharsetName();
    }

    @Override
    public void setDefaultCharsetName(String name) {
        application().setDefaultCharsetName(name);
    }

    @Override
    public Charset getDefaultCharset() {
        return application().getDefaultCharset();
    }

    @Override
    public @Nullable Charset getDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile) {
        return application().getDefaultCharsetForPropertiesFiles(virtualFile);
    }

    @Override
    public void setDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile, @Nullable Charset charset) {
        application().setDefaultCharsetForPropertiesFiles(virtualFile, charset);
    }

    @Override
    public @Nullable Charset getEncoding(@Nullable VirtualFile virtualFile, boolean useParentDefaults) {
        return application().getEncoding(virtualFile, useParentDefaults);
    }

    @Override
    public void setEncoding(@Nullable VirtualFile virtualFileOrDir, @Nullable Charset charset) {
        application().setEncoding(virtualFileOrDir, charset);
    }

    @Override
    public Charset getDefaultConsoleEncoding() {
        return application().getDefaultConsoleEncoding();
    }
}
