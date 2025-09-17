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
package consulo.awt.hacking;

import sun.awt.datatransfer.DataTransferer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorTable;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2023-08-14
 */
public class DataTransfererHacking {
    public static Set<DataFlavor> getFlavorsForFormats(long[] formats, FlavorTable map) {
        return DataTransferer.getInstance().getFlavorsForFormats(formats, map).keySet();
    }
}
