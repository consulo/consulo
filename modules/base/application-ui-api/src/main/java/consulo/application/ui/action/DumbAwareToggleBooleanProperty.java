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
package consulo.application.ui.action;

import consulo.application.dumb.DumbAware;
import consulo.application.ui.action.ToggleBooleanProperty;
import consulo.component.util.config.AbstractProperty;
import consulo.component.util.config.BooleanProperty;
import consulo.ui.image.Image;

public class DumbAwareToggleBooleanProperty extends ToggleBooleanProperty implements DumbAware {
  public DumbAwareToggleBooleanProperty(String text,
                                        String description,
                                        Image icon,
                                        AbstractProperty.AbstractPropertyContainer properties, BooleanProperty property) {
    super(text, description, icon, properties, property);
  }
}
