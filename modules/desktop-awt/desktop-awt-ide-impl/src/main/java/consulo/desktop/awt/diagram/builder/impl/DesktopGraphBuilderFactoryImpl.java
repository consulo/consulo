/*
 * Copyright 2013-2016 consulo.io
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
package consulo.desktop.awt.diagram.builder.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.diagram.builder.GraphBuilder;
import consulo.ide.impl.diagram.builder.GraphBuilderFactory;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 22:37/15.10.13
 */
@Singleton
@ServiceImpl
public class DesktopGraphBuilderFactoryImpl extends GraphBuilderFactory {
  @Override
  public GraphBuilder createBuilder() {
    return new DesktopGraphBuilderImpl();
  }
}
