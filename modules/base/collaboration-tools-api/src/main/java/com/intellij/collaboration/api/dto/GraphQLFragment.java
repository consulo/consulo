// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.api.dto;

import java.lang.annotation.*;

/**
 * Informational/marker annotation to link classes to fragment files.
 */
@Repeatable(GraphQLFragments.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphQLFragment {
    /**
     * The resource classpath where the referenced fragment is located.
     * This is used for navigation and manual comparison.
     */
    String filePath();
}
