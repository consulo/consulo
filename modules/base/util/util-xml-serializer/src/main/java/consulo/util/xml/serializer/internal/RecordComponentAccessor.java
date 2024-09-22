/*
 * Copyright 2013-2024 consulo.io
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

import consulo.util.xml.serializer.XmlSerializationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-09-22
 */
@SuppressWarnings("unchecked")
public class RecordComponentAccessor implements MutableAccessor {
    private final RecordComponent myRecordComponent;

    public RecordComponentAccessor(RecordComponent recordComponent) {
        myRecordComponent = recordComponent;
    }

    @Override
    public void set(@Nonnull Object host, @Nullable Object value) {
        ((Map) host).put(myRecordComponent.getName(), value);
    }

    @Override
    public void setBoolean(@Nonnull Object host, boolean value) {
        ((Map) host).put(myRecordComponent.getName(), value);
    }

    @Override
    public void setInt(@Nonnull Object host, int value) {
        ((Map) host).put(myRecordComponent.getName(), value);
    }

    @Override
    public void setShort(@Nonnull Object host, short value) {
        ((Map) host).put(myRecordComponent.getName(), value);
    }

    @Override
    public void setLong(@Nonnull Object host, long value) {
        ((Map) host).put(myRecordComponent.getName(), value);
    }

    @Override
    public void setDouble(@Nonnull Object host, double value) {
        ((Map) host).put(myRecordComponent.getName(), value);
    }

    @Override
    public void setFloat(@Nonnull Object host, float value) {
        ((Map) host).put(myRecordComponent.getName(), value);
    }

    @Override
    public Object read(@Nonnull Object o) {
        try {
            return myRecordComponent.getAccessor().invoke(o);
        }
        catch (Exception e) {
            throw new XmlSerializationException("Reading " + myRecordComponent.getName(), e);
        }
    }

    @Override
    public <T extends Annotation> T getAnnotation(@Nonnull Class<T> annotationClass) {
        return myRecordComponent.getAnnotation(annotationClass);
    }

    @Override
    public String getName() {
        return myRecordComponent.getName();
    }

    @Override
    public Class<?> getValueClass() {
        return myRecordComponent.getType();
    }

    @Override
    public Type getGenericType() {
        return myRecordComponent.getGenericType();
    }

    @Override
    public boolean isFinal() {
        return false;
    }
}
