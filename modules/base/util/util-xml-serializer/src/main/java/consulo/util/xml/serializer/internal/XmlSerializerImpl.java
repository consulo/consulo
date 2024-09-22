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

import consulo.util.lang.Pair;
import consulo.util.xml.serializer.JDOMExternalizableStringList;
import consulo.util.xml.serializer.SerializationFilter;
import consulo.util.xml.serializer.XmlSerializationException;
import consulo.util.xml.serializer.annotation.CollectionBean;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XmlSerializerImpl {
    private static Reference<Map<Pair<Type, MutableAccessor>, Binding>> ourBindings;

    @Nonnull
    public static Element serialize(@Nonnull Object object, @Nonnull SerializationFilter filter) throws XmlSerializationException {
        try {
            Class<?> aClass = object.getClass();
            Binding binding = getClassBinding(aClass, aClass, null);
            if (binding instanceof BeanBinding) {
                // top level expects not null (null indicates error, empty element will be omitted)
                return ((BeanBinding) binding).serialize(object, true, filter);
            }
            else {
                //noinspection ConstantConditions
                return (Element) binding.serialize(object, null, filter);
            }
        }
        catch (XmlSerializationException e) {
            throw e;
        }
        catch (Exception e) {
            throw new XmlSerializationException("Can't serialize instance of " + object.getClass(), e);
        }
    }

    @Nullable
    public static Element serializeIfNotDefault(@Nonnull Object object, @Nonnull SerializationFilter filter) {
        Class<?> aClass = object.getClass();
        Binding binding = getClassBinding(aClass, aClass, null);
        assert binding != null;
        return (Element) binding.serialize(object, null, filter);
    }

    @Nullable
    public static Binding getBinding(@Nonnull Type type) {
        return getClassBinding(typeToClass(type), type, null);
    }

    @Nullable
    public static Binding getBinding(@Nonnull MutableAccessor accessor) {
        Type type = accessor.getGenericType();
        return getClassBinding(typeToClass(type), type, accessor);
    }

    @Nonnull
    public static Class<?> typeToClass(@Nonnull Type type) {
        if (type instanceof Class) {
            return (Class<?>) type;
        }
        else if (type instanceof TypeVariable) {
            Type bound = ((TypeVariable) type).getBounds()[0];
            return bound instanceof Class ? (Class) bound : (Class<?>) ((ParameterizedType) bound).getRawType();
        }
        else {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        }
    }

    @Nullable
    public static synchronized Binding getClassBinding(@Nonnull Class<?> aClass, @Nonnull Type originalType, @Nullable MutableAccessor accessor) {
        if (aClass.isPrimitive() ||
            aClass == String.class ||
            aClass == Integer.class ||
            aClass == Long.class ||
            aClass == Boolean.class ||
            aClass == Double.class ||
            aClass == Float.class ||
            aClass.isEnum() ||
            Date.class.isAssignableFrom(aClass)) {
            return null;
        }

        Pair<Type, MutableAccessor> key = Pair.create(originalType, accessor);
        Map<Pair<Type, MutableAccessor>, Binding> map = getBindingCacheMap();
        Binding binding = map.get(key);
        if (binding == null) {
            binding = getNonCachedClassBinding(aClass, accessor, originalType);
            map.put(key, binding);
            try {
                binding.init(originalType);
            }
            catch (XmlSerializationException e) {
                map.remove(key);
                throw e;
            }
        }
        return binding;
    }

    @Nonnull
    private static Map<Pair<Type, MutableAccessor>, Binding> getBindingCacheMap() {
        Map<Pair<Type, MutableAccessor>, Binding> map = consulo.util.lang.ref.SoftReference.dereference(ourBindings);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            ourBindings = new SoftReference<>(map);
        }
        return map;
    }

    @Nonnull
    private static Binding getNonCachedClassBinding(@Nonnull Class<?> aClass, @Nullable MutableAccessor accessor, @Nonnull Type originalType) {
        if (aClass.isArray()) {
            if (Element.class.isAssignableFrom(aClass.getComponentType())) {
                assert accessor != null;
                return new JDOMElementBinding(accessor);
            }
            else {
                return new ArrayBinding(aClass, accessor);
            }
        }
        if (Collection.class.isAssignableFrom(aClass) && originalType instanceof ParameterizedType) {
            if (accessor != null) {
                CollectionBean listBean = accessor.getAnnotation(CollectionBean.class);
                if (listBean != null) {
                    return new CompactCollectionBinding(accessor);
                }
            }
            return new CollectionBinding((ParameterizedType) originalType, accessor);
        }
        if (accessor != null) {
            if (Map.class.isAssignableFrom(aClass) && originalType instanceof ParameterizedType) {
                return new MapBinding(accessor);
            }
            if (Element.class.isAssignableFrom(aClass)) {
                return new JDOMElementBinding(accessor);
            }

            if (JDOMExternalizableStringList.class.equals(aClass)) {
                return new CompactCollectionBinding(accessor);
            }
        }
        return new BeanBinding(aClass, accessor);
    }

    @Nullable
    static Object convert(@Nullable String value, @Nonnull Class<?> valueClass) {
        if (value == null) {
            return null;
        }
        else if (valueClass == String.class) {
            return value;
        }
        else if (valueClass == int.class || valueClass == Integer.class) {
            return Integer.parseInt(value);
        }
        else if (valueClass == boolean.class || valueClass == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        else if (valueClass == double.class || valueClass == Double.class) {
            return Double.parseDouble(value);
        }
        else if (valueClass == float.class || valueClass == Float.class) {
            return Float.parseFloat(value);
        }
        else if (valueClass == long.class || valueClass == Long.class) {
            return Long.parseLong(value);
        }
        else if (valueClass.isEnum()) {
            for (Object enumConstant : valueClass.getEnumConstants()) {
                if (enumConstant.toString().equals(value)) {
                    return enumConstant;
                }
            }
            return null;
        }
        else if (Date.class.isAssignableFrom(valueClass)) {
            try {
                return new Date(Long.parseLong(value));
            }
            catch (NumberFormatException e) {
                return new Date(0);
            }
        }
        else {
            return value;
        }
    }

    static void doSet(@Nonnull Object host, @Nullable String value, @Nonnull MutableAccessor accessor, @Nonnull Class<?> valueClass) {
        if (value == null) {
            accessor.set(host, null);
        }
        else if (valueClass == String.class) {
            accessor.set(host, value);
        }
        else if (valueClass == int.class) {
            accessor.setInt(host, Integer.parseInt(value));
        }
        else if (valueClass == boolean.class) {
            accessor.setBoolean(host, Boolean.parseBoolean(value));
        }
        else if (valueClass == double.class) {
            accessor.setDouble(host, Double.parseDouble(value));
        }
        else if (valueClass == float.class) {
            accessor.setFloat(host, Float.parseFloat(value));
        }
        else if (valueClass == long.class) {
            accessor.setLong(host, Long.parseLong(value));
        }
        else if (valueClass == short.class) {
            accessor.setShort(host, Short.parseShort(value));
        }
        else if (valueClass.isEnum()) {
            Object deserializedValue = null;
            for (Object enumConstant : valueClass.getEnumConstants()) {
                if (enumConstant.toString().equals(value)) {
                    deserializedValue = enumConstant;
                }
            }
            accessor.set(host, deserializedValue);
        }
        else if (Date.class.isAssignableFrom(valueClass)) {
            try {
                accessor.set(host, new Date(Long.parseLong(value)));
            }
            catch (NumberFormatException e) {
                accessor.set(host, new Date(0));
            }
        }
        else {
            Object deserializedValue = value;
            if (valueClass == Boolean.class) {
                deserializedValue = Boolean.parseBoolean(value);
            }
            else if (valueClass == Integer.class) {
                deserializedValue = Integer.parseInt(value);
            }
            else if (valueClass == Short.class) {
                deserializedValue = Short.parseShort(value);
            }
            else if (valueClass == Long.class) {
                deserializedValue = Long.parseLong(value);
            }
            else if (valueClass == Double.class) {
                deserializedValue = Double.parseDouble(value);
            }
            else if (valueClass == Float.class) {
                deserializedValue = Float.parseFloat(value);
            }
            accessor.set(host, deserializedValue);
        }
    }

    @Nonnull
    static String convertToString(@Nonnull Object value) {
        if (value instanceof Date) {
            return Long.toString(((Date) value).getTime());
        }
        else {
            return value.toString();
        }
    }

    @Nonnull
    static String getTextValue(@Nonnull Element element, @Nonnull String defaultText) {
        List<Content> content = element.getContent();
        String value = defaultText;
        if (!content.isEmpty()) {
            Content child = content.get(0);
            if (child instanceof Text) {
                value = child.getValue();
            }
        }
        return value;
    }
}
