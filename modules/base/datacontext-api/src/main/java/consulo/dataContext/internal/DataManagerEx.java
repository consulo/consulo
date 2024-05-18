/*
 * Copyright 2013-2024 consulo.io
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
package consulo.dataContext.internal;

import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;

/**
 * @author VISTALL
 * @since 12.05.2024
 */
public interface DataManagerEx extends DataManager {
  default DataProvider getDataProviderEx(java.awt.Component component) {
    throw new UnsupportedOperationException();
  }
}
