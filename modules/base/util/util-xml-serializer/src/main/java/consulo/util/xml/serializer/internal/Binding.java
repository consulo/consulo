/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.util.xml.serializer.internal;

import consulo.util.xml.serializer.SerializationFilter;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

public abstract class Binding {
    protected static final Logger LOG = LoggerFactory.getLogger(Binding.class);

    public abstract @Nullable MutableAccessor getAccessor();

    public abstract @Nullable Object serialize(Object o, @Nullable Object context, SerializationFilter filter);

    public @Nullable Object deserialize(@Nullable Object context, Element element) {
        return context;
    }

    public boolean isBoundTo(Element element) {
        return false;
    }

    void init(Type originalType) {
        // called (and make sense) only if MainBinding
    }

    @Nullable
    @SuppressWarnings("CastToIncompatibleInterface")
    public static Object deserializeList(Binding binding, Object context, List<Element> nodes) {
        if (binding instanceof MultiNodeBinding multiNodeBinding) {
            return multiNodeBinding.deserializeList(context, nodes);
        }
        else if (nodes.size() == 1) {
            return binding.deserialize(context, nodes.get(0));
        }
        else if (nodes.isEmpty()) {
            return null;
        }
        else {
            throw new AssertionError("Duplicate data for " + binding + " will be ignored");
        }
    }
}
