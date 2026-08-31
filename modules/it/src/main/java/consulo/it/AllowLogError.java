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
package consulo.it;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lets the annotated test log errors, which by default fail it.
 * <p>
 * A {@code Logger.error(...)} reports a broken invariant, so a test which reaches one has found a defect even when
 * every assertion in it passes. Recorded errors therefore fail the test they were logged in. Opting in is for flows
 * which are not free of them yet; new tests should not need this annotation.
 * <p>
 * {@link #value()} narrows the opt-in to the logger categories listed, matched by prefix, so that the test keeps
 * failing on any other error. An empty list tolerates every category.
 * <p>
 * Handled by {@link HeadlessApplicationExtension}, and can be placed on a test class or a single test method.
 *
 * @author VISTALL
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AllowLogError {
    String[] value() default {};
}
