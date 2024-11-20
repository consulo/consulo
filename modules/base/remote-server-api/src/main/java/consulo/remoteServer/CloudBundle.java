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
package consulo.remoteServer;

import consulo.annotation.internal.MigratedExtensionsTo;
import consulo.component.util.localize.AbstractBundle;
import consulo.remoteServer.localize.RemoteServerLocalize;
import consulo.util.collection.ArrayUtil;

/**
 * @author VISTALL
 * @since 2024-11-20
 */
@Deprecated
@MigratedExtensionsTo(RemoteServerLocalize.class)
public class CloudBundle extends AbstractBundle {
    private static final CloudBundle INSTANCE = new CloudBundle();

    private CloudBundle() {
        super("consulo.remoteServer.CloudBundle");
    }

    public static String message(String key) {
        return INSTANCE.getMessage(key, ArrayUtil.EMPTY_OBJECT_ARRAY);
    }

    public static String message(String key, Object... args) {
        return INSTANCE.getMessage(key, args);
    }
}
