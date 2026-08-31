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

/**
 * @author VISTALL
 * @since 2022-01-13
 */
module consulo.localize.impl {
    requires consulo.container.api;
    requires consulo.logging.api;
    requires consulo.localize.api;
    requires consulo.util.lang;
    requires consulo.util.io;
    requires consulo.proxy;
    requires com.google.protobuf;

    requires org.yaml.snakeyaml;
    requires com.ibm.icu;

    provides consulo.localize.LocalizeManager with consulo.localize.impl.LocalizeManagerImpl;
}