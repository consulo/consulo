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
 * Payload which will be available while handling click on the inlay.
 */
public class InlayActionData {
    private final InlayActionPayload payload;
    private final String handlerId;

    public InlayActionData(InlayActionPayload payload, String handlerId) {
        this.payload = payload;
        this.handlerId = handlerId;
    }

    public InlayActionPayload getPayload() {
        return payload;
    }

    public String getHandlerId() {
        return handlerId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof InlayActionData)) return false;
        InlayActionData that = (InlayActionData) other;
        if (!payload.equals(that.payload)) return false;
        return handlerId.equals(that.handlerId);
    }

    @Override
    public int hashCode() {
        int result = payload.hashCode();
        result = 31 * result + handlerId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "InlayActionData(payload=" + payload + ", handlerId='" + handlerId + "')";
    }
}
