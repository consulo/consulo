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
package consulo.ui.messageList;

/**
 * Who a message of a {@link MessageList} comes from, which decides how it stands out from the others.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
public enum MessageListItemStyle {
    /**
     * Written by the person using the list. Shown without the picture of its author - who that is goes without saying.
     */
    OWN,
    /**
     * Written by someone else - another person or an assistant.
     */
    OTHER,
    /**
     * Reported by the program rather than written - a tool run, a status. Shown without a picture, it has no author.
     */
    SYSTEM,
    ERROR
}
