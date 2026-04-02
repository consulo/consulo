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
package consulo.virtualFileSystem;

import consulo.annotation.access.RequiredWriteAction;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.encoding.EncodingRegistry;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Objects;

/**
 * @author UNV
 * @since 2026-03-31
 */
public abstract class BaseVirtualFile extends UserDataHolderBase implements VirtualFile {
    private static final Logger LOG = Logger.getInstance(VirtualFile.class);
    private static final Key<byte[]> BOM_KEY = Key.create("BOM");
    private static final Key<Charset> CHARSET_KEY = Key.create("CHARSET");
    private static final Key<String> DETECTED_LINE_SEPARATOR_KEY = Key.create("DETECTED_LINE_SEPARATOR_KEY");

    protected BaseVirtualFile() {
    }

    /**
     * @inheritDoc
     */
    @Override
    @RequiredWriteAction
    public void delete(@Nullable Object requestor) throws IOException {
        LOG.assertTrue(isValid(), "Deleting invalid file");
        getFileSystem().deleteFile(requestor, this);
    }

    /**
     * @inheritDoc
     */
    @Override
    public Charset getCharset() {
        Charset charset = getStoredCharset();
        if (charset == null) {
            charset = EncodingRegistry.getInstance().getDefaultCharset();
            setCharset(charset);
        }
        return charset;
    }

    protected @Nullable Charset getStoredCharset() {
        return getUserData(CHARSET_KEY);
    }

    protected void storeCharset(@Nullable Charset charset) {
        putUserData(CHARSET_KEY, charset);
    }

    @Override
    public void setCharset(@Nullable Charset charset, @Nullable Runnable whenChanged, boolean fireEventsWhenChanged) {
        Charset old = getStoredCharset();
        storeCharset(charset);
        if (Objects.equals(charset, old)) return;
        byte[] bom = charset == null ? null : CharsetToolkit.getMandatoryBom(charset);
        byte[] existingBOM = getBOM();
        if (bom == null && charset != null && existingBOM != null) {
            bom = CharsetToolkit.canHaveBom(charset, existingBOM) ? existingBOM : null;
        }
        setBOM(bom);

        if (old != null) { //do not send on detect
            if (whenChanged != null) whenChanged.run();
            if (fireEventsWhenChanged) {
                VirtualFileManager.getInstance().notifyPropertyChanged(this, PROP_ENCODING, old, charset);
            }
        }
    }

    @Override
    public boolean isCharsetSet() {
        return getStoredCharset() != null;
    }

    @Override
    public byte @Nullable [] getBOM() {
        return getUserData(BOM_KEY);
    }

    @Override
    public void setBOM(byte @Nullable [] BOM) {
        putUserData(BOM_KEY, BOM);
    }

    @Override
    public String toString() {
        return "VirtualFile: " + getPresentableUrl();
    }

    /**
     * @inheritDoc
     */
    @Override
    public @Nullable String getDetectedLineSeparator() {
        return getUserData(DETECTED_LINE_SEPARATOR_KEY);
    }

    @Override
    public void setDetectedLineSeparator(@Nullable String separator) {
        putUserData(DETECTED_LINE_SEPARATOR_KEY, separator);
    }
}
