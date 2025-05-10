/*
 * Copyright 2013-2025 consulo.io
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
package consulo.annotation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;

import java.lang.annotation.*;

/**
 * Annotation for lambda parameter specifying that this lambda is executed in the same @RRA/RWA/RUI context with caller method.
 *
 * @see RequiredReadAction
 * @see RequiredWriteAction
 *
 * @author UNV
 * @since 2025-05-10
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface InheritCallerContext {
}
