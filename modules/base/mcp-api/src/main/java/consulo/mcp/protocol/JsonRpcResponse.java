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
package consulo.mcp.protocol;

import com.google.gson.JsonElement;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-08-03
 */
public final class JsonRpcResponse {
    public static final String VERSION = "2.0";

    public static JsonRpcResponse success(@Nullable JsonElement id, @Nullable Object result) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.id = id;
        response.result = result;
        return response;
    }

    public static JsonRpcResponse error(@Nullable JsonElement id, int code, String message) {
        JsonRpcResponse response = new JsonRpcResponse();
        response.id = id;
        response.error = new JsonRpcError(code, message);
        return response;
    }

    public String jsonrpc = VERSION;
    public @Nullable JsonElement id;
    public @Nullable Object result;
    public @Nullable JsonRpcError error;
}
