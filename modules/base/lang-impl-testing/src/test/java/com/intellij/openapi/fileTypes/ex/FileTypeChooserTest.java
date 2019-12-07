/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.openapi.fileTypes.ex;

import consulo.testFramework.AssertEx;
import org.junit.Test;

/**
 * @author Nikolay Matveev
 */
public class FileTypeChooserTest {
  @Test
  public void testSuggestPatterns() {
    AssertEx.assertSameElements(FileTypeChooser.suggestPatterns("a"), "a");
    AssertEx.assertSameElements(FileTypeChooser.suggestPatterns("a.b"), "a.b", "*.b");
    AssertEx.assertSameElements(FileTypeChooser.suggestPatterns("a.b.c"), "a.b.c", "*.c", "*.b.c");
  }
}

