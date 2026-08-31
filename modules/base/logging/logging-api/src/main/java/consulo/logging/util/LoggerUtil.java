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
import consulo.util.lang.StringUtil;

/**
 * @author VISTALL
 * @since 2022-03-20
 */
public class LoggerUtil {
    public static void error(Logger logger, String message, String... attachmentText) {
        error(logger, message, new Throwable(), attachmentText);
    }

    @SuppressWarnings("ConstantConditions")
    public static void error(Logger logger, String message, Throwable cause, String... attachmentText) {
        if (attachmentText != null && attachmentText.length != 0) {
            logger.error(message, cause, AttachmentFactory.get().create("current-context.txt", StringUtil.join(attachmentText, ",")));
        }
        else {
            logger.error(message, cause, Attachment.EMPTY_ARRAY);
        }
    }
}
