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
package consulo.collaboration.api.json;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;

/**
 * Deserializes JSON data into typed objects.
 */
public interface JsonDataDeserializer {
    <T> T fromJson(Reader reader, Type type) throws IOException;
}
