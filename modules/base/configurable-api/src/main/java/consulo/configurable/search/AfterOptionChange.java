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
 * Marks a static method of a configurable, called after one of its options is switched without the page - what the
 * page's {@code apply()} does besides storing the value, such as telling the UI the settings changed.
 * <p/>
 * The method takes no parameter, or the settings object the option was stored in. It must not be private.
 * A configurable which overrides {@code apply()} and has no such method gets no {@link SearchableBooleanOptions}.
 *
 * @author VISTALL
 * @since 2026-11-11
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface AfterOptionChange {
}
