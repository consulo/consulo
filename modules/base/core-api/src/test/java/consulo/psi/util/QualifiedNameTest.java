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
package consulo.psi.util;

import com.intellij.psi.util.QualifiedName;
import junit.framework.TestCase;

/**
 * @author VISTALL
 * @since 18:41/28.12.13
 */
public class QualifiedNameTest extends TestCase {
  public void testQ1() {
    QualifiedName qualifiedName = QualifiedName.fromDottedString("parent1.parent2.parent3");
    QualifiedName parent = qualifiedName.getParent();

    assertNotNull(parent);
    assertEquals(parent, QualifiedName.fromDottedString("parent1.parent2"));
  }

  public void testQ2() {
    QualifiedName qualifiedName = QualifiedName.fromDottedString("parent1");
    QualifiedName parent = qualifiedName.getParent();

    assertNotNull(parent);
    assertEquals(parent, QualifiedName.fromDottedString(""));
  }

  public void testQ3() {
    QualifiedName qualifiedName = QualifiedName.fromDottedString("");
    QualifiedName parent = qualifiedName.getParent();

    assertNull(parent);
  }

  public void testQ4() {
    assertEquals(QualifiedName.fromComponents(), QualifiedName.fromDottedString(""));
    assertEquals(QualifiedName.ROOT, QualifiedName.fromDottedString(""));
    assertEquals(QualifiedName.ROOT, QualifiedName.fromComponents());
  }
}
