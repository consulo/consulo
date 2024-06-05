/*
 * Copyright 2013-2022 consulo.io
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
package consulo.annotation.component;

/**
 * Action reference. Can be search by {@link #id()} or {@link #type()}
 * <p>
 * If references by class - action must be registered, and annotated by @{@link ActionImpl}
 *
 * @author VISTALL
 * @since 17-Jul-22
 */
public @interface ActionRef {
  String NOT_DEFINED_ID = "<not-defined>";

  String id() default NOT_DEFINED_ID;

  Class<?> type() default Object.class;
}
