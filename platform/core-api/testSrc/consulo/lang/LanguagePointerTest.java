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
package consulo.lang;

import com.intellij.lang.Language;
import consulo.util.pointers.NamedPointer;
import junit.framework.TestCase;

/**
 * @author VISTALL
 * @since 18:44/31.08.13
 */
public class LanguagePointerTest extends TestCase{
  static {
    new Language("ID0") {};
    new Language("ID1") {};
    new Language("ID2") {};
    new Language("ID3") {};
    new Language("ID4") {};
    new Language("ID5") {};
    new Language("ID6") {};
  }

  @Override
  protected void runTest() throws Throwable {
    String name = getName();
    String languageId = name.substring(4, name.length());

    NamedPointer<Language> pointer = LanguagePointerUtil.createPointer(languageId);

    Language language = pointer.get();

    assertTrue(language != null);

    assertEquals(language.getID(), languageId);
  }

  public void testID0() {}
  public void testID1() {}
  public void testID2() {}
  public void testID3() {}
  public void testID4() {}
  public void testID5() {}
  public void testID6() {}
}
