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
package consulo.component.store.impl.internal.json;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonWriter;
import com.dslplatform.json.PrettifyOutputStream;
import com.dslplatform.json.runtime.ArrayAnalyzer;
import com.dslplatform.json.runtime.CollectionAnalyzer;
import com.dslplatform.json.runtime.EnumAnalyzer;
import com.dslplatform.json.runtime.MapAnalyzer;
import consulo.container.classloader.ClassLoaderDataKey;
import consulo.container.classloader.PluginClassLoader;
import consulo.logging.Logger;
import consulo.util.io.UnsyncByteArrayOutputStream;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.function.UnaryOperator;

/**
 * @author VISTALL
 * @since 2026-09-05
 */
public final class StateJsonBinder {
    private static final Logger LOG = Logger.getInstance(StateJsonBinder.class);

    private static final ClassLoaderDataKey<DslJson<Object>> KEY = new ClassLoaderDataKey<>("state-json-binder");

    private static final Map<ClassLoader, DslJson<Object>> ourSharedBinders = Collections.synchronizedMap(new WeakHashMap<>());

    private static final Set<Class<?>> ourNotSerializable = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private static final DslJson<Object> ourEnvelopeBinder = new DslJson<>(defaultSettings());

    private static final byte[] EMPTY_OBJECT = {'{', '}'};

    private static final ClassValue<Map<String, Object>> ourDefaultStates = new ClassValue<>() {
        @Override
        protected Map<String, Object> computeValue(Class<?> type) {
            try {
                Object defaultState = binderFor(type).deserialize(type, EMPTY_OBJECT, EMPTY_OBJECT.length);
                if (defaultState == null) {
                    return Map.of();
                }

                UnsyncByteArrayOutputStream out = new UnsyncByteArrayOutputStream();
                binderFor(type).serialize(defaultState, out);
                return toObject(out.toByteArray());
            }
            catch (Throwable e) {
                LOG.warn("Unable to compute default json state of " + type.getName(), e);
                return Map.of();
            }
        }
    };

    private StateJsonBinder() {
    }

    public static boolean isJsonCapable(Class<?> stateClass) {
        if (!stateClass.isAnnotationPresent(CompiledJson.class) || ourNotSerializable.contains(stateClass)) {
            return false;
        }

        DslJson<Object> binder = binderFor(stateClass);
        if (binder.tryFindWriter(stateClass) != null && binder.tryFindReader(stateClass) != null) {
            return true;
        }

        LOG.warn("State " + stateClass.getName() + " is annotated by @CompiledJson, but no converter is registered. " +
            "Module of state must declare 'provides com.dslplatform.json.Configuration' - check 'dsljson.configuration' property of its pom");
        return false;
    }

    public static byte[] serialize(Object state) throws IOException {
        UnsyncByteArrayOutputStream out = new UnsyncByteArrayOutputStream();
        binderFor(state.getClass()).serialize(state, out);
        return skipDefaultValues(state.getClass(), out.toByteArray());
    }

    public static void markNotSerializable(Class<?> stateClass, Throwable cause) {
        if (ourNotSerializable.add(stateClass)) {
            LOG.warn("State " + stateClass.getName() + " stays xml - it is annotated by @CompiledJson but its object graph is not fully " +
                "json capable. Annotate every nested type by @CompiledJson.", cause);
        }
    }

    public static boolean isEmptyState(byte[] content) {
        return content.length == 0 || Arrays.equals(content, EMPTY_OBJECT);
    }

    private static byte[] skipDefaultValues(Class<?> stateClass, byte[] content) throws IOException {
        Map<String, Object> defaultState = ourDefaultStates.get(stateClass);
        if (defaultState.isEmpty()) {
            return content;
        }

        Map<String, Object> actual = toObject(content);
        if (actual.isEmpty()) {
            return content;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : actual.entrySet()) {
            if (!Objects.equals(defaultState.get(entry.getKey()), entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        if (result.size() == actual.size()) {
            return content;
        }

        UnsyncByteArrayOutputStream out = new UnsyncByteArrayOutputStream();
        ourEnvelopeBinder.serialize(result, out);
        return out.toByteArray();
    }

    private static Map<String, Object> toObject(byte[] content) throws IOException {
        if (content.length == 0 || content[0] != '{') {
            return Map.of();
        }

        Map<?, ?> parsed = ourEnvelopeBinder.deserialize(Map.class, content, content.length);
        if (parsed == null) {
            return Map.of();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : parsed.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    public static @Nullable <T> T deserialize(Class<T> stateClass, byte[] content) throws IOException {
        return binderFor(stateClass).deserialize(stateClass, content, content.length);
    }

    public static byte[] writeEnvelope(Map<String, byte[]> states) {
        JsonWriter writer = ourEnvelopeBinder.newWriter();
        writer.writeByte(JsonWriter.OBJECT_START);

        boolean first = true;
        for (Map.Entry<String, byte[]> entry : new TreeMap<>(states).entrySet()) {
            if (!first) {
                writer.writeByte(JsonWriter.COMMA);
            }
            first = false;

            writer.writeString(entry.getKey());
            writer.writeByte(JsonWriter.SEMI);
            writer.writeAscii(entry.getValue());
        }

        writer.writeByte(JsonWriter.OBJECT_END);
        return writer.toByteArray();
    }

    public static byte[] prettyPrint(byte[] content) throws IOException {
        UnsyncByteArrayOutputStream out = new UnsyncByteArrayOutputStream();
        try (PrettifyOutputStream pretty = new PrettifyOutputStream(out, PrettifyOutputStream.IndentType.SPACES, 2)) {
            pretty.write(content, 0, content.length);
        }
        return out.toByteArray();
    }

    public static Map<String, byte[]> readEnvelope(byte[] content) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<>();
        if (content.length == 0) {
            return result;
        }

        Map<?, ?> envelope = ourEnvelopeBinder.deserialize(Map.class, content, content.length);
        if (envelope == null) {
            return result;
        }

        for (Map.Entry<?, ?> entry : envelope.entrySet()) {
            UnsyncByteArrayOutputStream out = new UnsyncByteArrayOutputStream();
            ourEnvelopeBinder.serialize(entry.getValue(), out);
            result.put(String.valueOf(entry.getKey()), out.toByteArray());
        }
        return result;
    }

    public static byte[] substituteStrings(byte[] envelope, UnaryOperator<String> operator) throws IOException {
        if (envelope.length == 0) {
            return envelope;
        }

        Map<?, ?> tree = ourEnvelopeBinder.deserialize(Map.class, envelope, envelope.length);
        if (tree == null) {
            return envelope;
        }

        UnsyncByteArrayOutputStream out = new UnsyncByteArrayOutputStream();
        ourEnvelopeBinder.serialize(substitute(tree, operator, false), out);
        return out.toByteArray();
    }

    private static Object substitute(@Nullable Object node, UnaryOperator<String> operator, boolean substituteKeys) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                result.put(substituteKeys ? operator.apply(key) : key, substitute(entry.getValue(), operator, true));
            }
            return result;
        }

        if (node instanceof List<?> list) {
            List<Object> result = new ArrayList<>(list.size());
            for (Object item : list) {
                result.add(substitute(item, operator, true));
            }
            return result;
        }

        if (node instanceof String value) {
            return operator.apply(value);
        }

        return node;
    }

    private static DslJson<Object> binderFor(Class<?> stateClass) {
        ClassLoader classLoader = stateClass.getClassLoader();

        if (classLoader instanceof PluginClassLoader pluginClassLoader) {
            return pluginClassLoader.getOrCreateData(KEY, () -> create(classLoader));
        }

        return ourSharedBinders.computeIfAbsent(classLoader, StateJsonBinder::create);
    }

    private static DslJson<Object> create(ClassLoader classLoader) {
        return new DslJson<>(defaultSettings().includeServiceLoader(classLoader));
    }

    private static <T> DslJson.Settings<T> defaultSettings() {
        return new DslJson.Settings<T>()
            .resolveReader(MapAnalyzer.READER)
            .resolveWriter(MapAnalyzer.WRITER)
            .resolveReader(CollectionAnalyzer.READER)
            .resolveWriter(CollectionAnalyzer.WRITER)
            .resolveReader(ArrayAnalyzer.READER)
            .resolveWriter(ArrayAnalyzer.WRITER)
            .resolveWriter(EnumAnalyzer.CONVERTER)
            .resolveReader(EnumAnalyzer.CONVERTER);
    }
}
