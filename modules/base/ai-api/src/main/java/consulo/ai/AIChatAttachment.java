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
package consulo.ai;

import consulo.localize.LocalizeValue;

import java.util.List;

/**
 * Something the user gave a chat to go with the next message - a file, a selection of one, an image - shown as a chip
 * over the message box until the message is sent.
 *
 * @param content what the message carries for it
 * @author VISTALL
 * @since 2026-11-13
 */
public record AIChatAttachment(LocalizeValue label, List<AIContent> content) {
}
