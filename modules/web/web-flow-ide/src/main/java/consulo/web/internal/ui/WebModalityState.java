/*
 * Copyright 2013-2023 consulo.io
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
package consulo.web.internal.ui;

import consulo.application.impl.internal.IdeaModalityStateEx;
import consulo.ui.ModalityState;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2023-11-10
 */
public class WebModalityState extends IdeaModalityStateEx implements ModalityState {
  public static final WebModalityState INSTANCE = new WebModalityState();
}
