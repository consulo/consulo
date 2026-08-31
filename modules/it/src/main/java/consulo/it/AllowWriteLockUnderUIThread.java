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
 * Lets the annotated test take the write lock from the UI thread, which by default fails the test.
 * <p>
 * Taking the write lock on the UI thread parks it until every reader releases the read lock - the shape of a
 * real freeze - so it is rejected everywhere unless a test opts back in. Opting in is for flows which are not
 * free of it yet; new tests should not need this annotation.
 * <p>
 * Handled by {@link HeadlessApplicationExtension}, and can be placed on a test class or a single test method.
 *
 * @author VISTALL
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AllowWriteLockUnderUIThread {
}
