/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.codeStyle;

import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SoftMargins implements Cloneable {
    private List<Integer> myValues;

    @SuppressWarnings("unused") // Serialization getter
    @Nullable
    public String getSOFT_MARGINS() {
        return myValues != null ? toString() : null;
    }

    @SuppressWarnings("unused") // Serialization setter
    public void setSOFT_MARGINS(@Nullable String valueList) {
        if (valueList != null) {
            String[] values = valueList.split(",\\s*");
            myValues = new ArrayList<>(values.length);
            int i = 0;
            for (String value : values) {
                try {
                    myValues.add(Integer.parseInt(value));
                }
                catch (NumberFormatException nfe) {
                    myValues = null;
                    return;
                }
            }
            Collections.sort(myValues);
        }
    }

    @Nonnull
    List<Integer> getValues() {
        return myValues != null ? myValues : Collections.emptyList();
    }

    void setValues(List<Integer> values) {
        if (values != null) {
            myValues = new ArrayList<>(values);
            Collections.sort(myValues);
        }
        else {
            myValues = null;
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Object clone() {
        SoftMargins copy = new SoftMargins();
        copy.setValues(myValues);
        return copy;
    }

    @Override
    @SuppressWarnings("EqualsHashCode")
    public boolean equals(Object obj) {
        if (obj instanceof SoftMargins softMargins) {
            List<Integer> otherMargins = softMargins.getValues();
            return otherMargins.equals(getValues());
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (myValues != null) {
            for (int margin : myValues) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(margin);
            }
        }
        return sb.toString();
    }

    public void serializeInto(@Nonnull Element element) {
        if (myValues != null && myValues.size() > 0) {
            XmlSerializer.serializeInto(this, element);
        }
    }

    public void deserializeFrom(@Nonnull Element element) {
        XmlSerializer.deserializeInto(this, element);
    }
}
