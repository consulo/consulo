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
package consulo.configurable.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tells the compile time generation of {@link SearchableConfigurableTexts} what it can't find in the configurable
 * itself.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface SearchableTexts {
    /**
     * Classes whose texts are on the page too - a panel or a helper the configurable builds its page with.
     * Their source must be compiled together with the configurable.
     */
    Class<?>[] value() default {};

    /**
     * The id of the page, for a configurable whose {@code getId()} returns no constant.
     */
    String id() default "";
}
