/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.inlay;

/**
 * The payload for the whole inlay. It may be used later by the actions from the right-click menu.
 */
public class InlayPayload {
    private final String payloadName;
    private final InlayActionPayload payload;

    public InlayPayload(String payloadName, InlayActionPayload payload) {
        this.payloadName = payloadName;
        this.payload = payload;
    }

    public String getPayloadName() {
        return payloadName;
    }

    public InlayActionPayload getPayload() {
        return payload;
    }
}
