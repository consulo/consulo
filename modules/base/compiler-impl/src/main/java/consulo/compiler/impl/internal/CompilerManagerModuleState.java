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
package consulo.compiler.impl.internal;

import com.dslplatform.json.CompiledJson;
import com.sun.jna.platform.win32.COM.util.annotation.ComObject;
import consulo.util.collection.Lists;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Property;
import consulo.util.xml.serializer.annotation.Tag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-09-08
 */
@CompiledJson
@Tag("module")
public class CompilerManagerModuleState {
    @Attribute
    public String name;

    @Attribute
    public boolean exclude = true;

    @Attribute
    public boolean inherit = true;

    @Property(surroundWithTag = false)
    @AbstractCollection(surroundWithTag = false)
    public List<CompilerManagerOutputState> outputs = new ArrayList<>();

    public CompilerManagerOutputState findByType(String typeId) {
        for (CompilerManagerOutputState output : outputs) {
            if (Objects.equals(output.type, typeId)) {
                return output;
            }
        }
        return null;
    }

    public void add(CompilerManagerOutputState state) {
        outputs.add(state);
        Lists.quickSort(outputs, Comparator.comparing(it -> it.type));
    }
}
