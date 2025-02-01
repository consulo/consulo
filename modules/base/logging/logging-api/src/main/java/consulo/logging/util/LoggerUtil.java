/*
 * Copyright 2013-2022 consulo.io
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
package consulo.logging.util;

import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 20-Mar-22
 */
public class LoggerUtil {
    public static void error(@Nonnull Logger logger, @Nonnull String message, @Nonnull String... attachmentText) {
        error(logger, message, new Throwable(), attachmentText);
    }

    public static void error(@Nonnull Logger logger, @Nonnull String message, @Nonnull Throwable cause, @Nonnull String... attachmentText) {
        StringBuilder detailsBuffer = new StringBuilder();
        for (String detail : attachmentText) {
            detailsBuffer.append(detail).append(",");
        }
        if (attachmentText.length > 0 && detailsBuffer.length() > 0) {
            detailsBuffer.setLength(detailsBuffer.length() - 1);
        }
        Attachment attachment = detailsBuffer.length() > 0 ? AttachmentFactory.get().create("current-context.txt", detailsBuffer.toString()) : null;
        logger.error(message, cause, attachment);
    }
}
