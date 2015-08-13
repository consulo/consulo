/*
 * Copyright 2013 must-be.org
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
package org.consulo.vfs.backgroundTask;

import org.mustbe.consulo.DeprecationInfo;

/**
 * @author VISTALL
 * @since 1:15/07.10.13
 */
@Deprecated
@DeprecationInfo(value = "Use org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeProvider", until = "1.0")
public abstract class BackgroundTaskByVfsChangeProvider
        extends org.mustbe.consulo.vfs.backgroundTask.BackgroundTaskByVfsChangeProvider {
}
