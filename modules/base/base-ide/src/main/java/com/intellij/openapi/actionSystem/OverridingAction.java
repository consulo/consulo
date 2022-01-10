// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.actionSystem;

import consulo.annotation.DeprecationInfo;

/**
 * Action intended to replace an existing action using the "overrides" attribute in plugin.xml and receiving an instance of the action
 * being overridden.
 *
 * @see com.intellij.openapi.actionSystem.impl.ActionManagerImpl#getBaseAction
 */
@Deprecated
@DeprecationInfo("Not supported. Only for correct merge from idea")
public interface OverridingAction {
}
