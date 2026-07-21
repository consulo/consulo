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
package consulo.it.internal.ui;

import consulo.ui.layout.Layout;
import consulo.ui.layout.LayoutConstraint;

/**
 * Base for dummy-but-creatable headless {@link Layout}s. All child-management methods on
 * {@link Layout} are {@code default} no-ops, so subclasses only need the component state provided by
 * {@link HeadlessComponentBase}; overriding {@code add} here would clash with the covariant
 * {@code add} of typed layouts such as {@code HorizontalLayout}.
 *
 * @author VISTALL
 */
public abstract class HeadlessLayoutBase<C extends LayoutConstraint> extends HeadlessComponentBase implements Layout<C> {
}
