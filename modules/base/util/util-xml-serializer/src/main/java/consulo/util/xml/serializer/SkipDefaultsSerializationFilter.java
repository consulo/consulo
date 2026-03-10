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
package consulo.util.xml.serializer;

import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.ThreeState;
import consulo.util.xml.serializer.internal.BasePrimitiveBinding;
import consulo.util.xml.serializer.internal.BeanBinding;
import consulo.util.xml.serializer.internal.Binding;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class SkipDefaultsSerializationFilter extends SkipDefaultValuesSerializationFilters {
    protected static final Logger LOG = LoggerFactory.getLogger(SkipDefaultsSerializationFilter.class);

    public boolean equal(Binding binding, Object bean) {
        Accessor accessor = Objects.requireNonNull(binding.getAccessor());
        return equal(binding, accessor.read(bean), accessor.read(getDefaultBean(bean)));
    }

    public boolean equal(@Nullable Binding binding, @Nullable Object currentValue, @Nullable Object defaultValue) {
        if (defaultValue instanceof Element && currentValue instanceof Element) {
            return JDOMUtil.areElementsEqual((Element) currentValue, (Element) defaultValue);
        }
        else {
            if (currentValue == defaultValue) {
                return true;
            }
            if (currentValue == null || defaultValue == null) {
                return false;
            }

            if (binding instanceof BasePrimitiveBinding) {
                Binding referencedBinding = ((BasePrimitiveBinding) binding).getBinding();
                if (referencedBinding instanceof BeanBinding) {
                    BeanBinding classBinding = (BeanBinding) referencedBinding;
                    ThreeState compareByFields = classBinding.hasEqualMethod;
                    if (compareByFields == ThreeState.UNSURE) {
                        try {
                            classBinding.myBeanClass.getDeclaredMethod("equals", Object.class);
                            compareByFields = ThreeState.NO;
                        }
                        catch (NoSuchMethodException ignored) {
                            compareByFields = ThreeState.YES;
                        }
                        catch (Exception e) {
                            LOG.warn(e.getMessage(), e);
                        }

                        classBinding.hasEqualMethod = compareByFields;
                    }

                    if (compareByFields == ThreeState.YES) {
                        return classBinding.equalByFields(currentValue, defaultValue, this);
                    }
                }
            }

            return Comparing.equal(currentValue, defaultValue);
        }
    }
}
