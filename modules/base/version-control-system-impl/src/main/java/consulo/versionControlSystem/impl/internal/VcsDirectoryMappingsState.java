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
package consulo.versionControlSystem.impl.internal;

import com.dslplatform.json.CompiledJson;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-09-08
 */
@CompiledJson
@Tag("state")
public class VcsDirectoryMappingsState {
    @Attribute
    public boolean defaultProject;

    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false)
    public List<VcsDirectoryMappingState> mappings = new ArrayList<>();
}
