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

import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.util.lang.ExceptionUtil;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Diagnostic attachments carry the interesting text of platform error reports (for example the PSI/stub trees of a
 * "stub tree and index do not match" report); without a {@link java.util.ServiceLoader} provider the factory holder
 * fails class initialization and the report itself is lost behind {@link NoClassDefFoundError}.
 *
 * @author VISTALL
 */
public class HeadlessAttachmentFactory implements AttachmentFactory {
    private static class HeadlessAttachment implements Attachment {
        private final String myPath;
        private final String myDisplayText;
        private boolean myIncluded = true;

        HeadlessAttachment(String path, String displayText) {
            myPath = path;
            myDisplayText = displayText;
        }

        @Override
        public String getDisplayText() {
            return myDisplayText;
        }

        @Override
        public String getPath() {
            return myPath;
        }

        @Override
        public String getName() {
            int lastSlash = myPath.lastIndexOf('/');
            return lastSlash < 0 ? myPath : myPath.substring(lastSlash + 1);
        }

        @Override
        public String getEncodedBytes() {
            return Base64.getEncoder().encodeToString(myDisplayText.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean isIncluded() {
            return myIncluded;
        }

        @Override
        public void setIncluded(Boolean included) {
            myIncluded = included;
        }

        @Override
        public Attachment copy(String newPath) {
            return new HeadlessAttachment(newPath, myDisplayText);
        }
    }

    @Override
    public Attachment create(String path, String content) {
        return new HeadlessAttachment(path, content);
    }

    @Override
    public Attachment create(String path, byte[] bytes, String displayText) {
        return new HeadlessAttachment(path, displayText);
    }

    @Override
    public Attachment create(String name, Throwable throwable) {
        return new HeadlessAttachment(name, ExceptionUtil.getThrowableText(throwable));
    }
}
