/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.web.internal.ex;

import com.vaadin.ui.AbstractComponent;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.Size;
import consulo.ui.internal.VaadinWrapper;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23-Sep-17
 */
public class WGwtLoadingPanelImpl extends AbstractComponent implements Component, VaadinWrapper {
  @RequiredUIAccess
  @Override
  public void setSize(@Nonnull Size size) {
  }
}
