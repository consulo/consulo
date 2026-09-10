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
package consulo.component.impl.internal;

import com.dslplatform.json.CompiledJson;
import consulo.util.xml.serializer.annotation.MapAnnotation;
import consulo.util.xml.serializer.annotation.Property;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-09-05
 */
@CompiledJson
public class PropertiesState {
    @Property(surroundWithTag = false)
    @MapAnnotation(
        surroundWithTag = false,
        surroundKeyWithTag = false,
        surroundValueWithTag = false,
        keyAttributeName = "name",
        valueAttributeName = "value",
        entryTagName = "property"
    )
    public Map<String, String> properties = new LinkedHashMap<>();
}
