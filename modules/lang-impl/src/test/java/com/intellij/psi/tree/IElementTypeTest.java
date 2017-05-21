/*
 * Copyright (c) 2000-2006 JetBrains s.r.o. All Rights Reserved.
 */
package com.intellij.psi.tree;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.mock.MockPsiFile;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import gnu.trove.THashMap;
import gnu.trove.TObjectIntHashMap;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author gregsh
 */
public class IElementTypeTest extends LightPlatformCodeInsightFixtureTestCase {

  // load all parser definitions, instantiate all lexers & parsers to initialize all IElementType constants
  @SuppressWarnings("UnusedDeclaration")
  public void testCount() throws Exception {
    int count = IElementType.getAllocatedTypesCount();
    System.out.println("Preloaded: " + count +" element types");
    LanguageExtensionPoint[] extensions = Extensions.getExtensions(new ExtensionPointName<LanguageExtensionPoint>("com.intellij.lang.parserDefinition"));
    System.out.println("ParserDefinitions: " + extensions.length);

    THashMap<Language, String> languageMap = new THashMap<Language, String>();
    languageMap.put(Language.ANY, "platform");
    final TObjectIntHashMap<String> map = new TObjectIntHashMap<String>();
    for (LanguageExtensionPoint e : extensions) {
      String key = e.getPluginDescriptor().getPluginId().getIdString();
      int curCount = IElementType.getAllocatedTypesCount();
      ParserDefinition definition = (ParserDefinition)e.getInstance();
      IFileElementType type = definition.getFileNodeType();
      Language language = type.getLanguage();
      languageMap.put(language, key);
      if (language.getBaseLanguage() != null && !languageMap.containsKey(language.getBaseLanguage())) {
        languageMap.put(language.getBaseLanguage(), key);
      }
      try {
        Lexer lexer = definition.createLexer(MockPsiFile.DUMMY_LANG_VERSION);
        PsiParser parser = definition.createParser(MockPsiFile.DUMMY_LANG_VERSION);
      }
      catch (UnsupportedOperationException e1) {
      }

      // language-based calculation: per-class-loading stuff commented
      //int diff = IElementType.getAllocatedTypesCount() - curCount;
      //map.put(key, map.get(key) + diff);
    }
    // language-based calculation
    count = IElementType.getAllocatedTypesCount();

    for (short i = 0; i < count; i ++ ) {
      IElementType type = IElementType.find(i);
      Language language = type.getLanguage();
      String key = null;
      for (Language cur = language; cur != null && key == null; cur = cur.getBaseLanguage()) {
        key = languageMap.get(cur);
      }
      key = StringUtil.notNullize(key, "unknown");
      map.put(key, map.get(key) + 1);
      //if (key.equals("unknown")) System.out.println(type +"   " + language);
    }
    System.out.println("Total: " + IElementType.getAllocatedTypesCount() +" element types");

    // Show per-plugin statistics
    Object[] keys = map.keys();
    Arrays.sort(keys, new Comparator<Object>() {
      @Override
      public int compare(Object o1, Object o2) {
        return map.get((String)o2) - map.get((String)o1);
      }
    });
    int sum = 0;
    for (Object key : keys) {
      int value = map.get((String)key);
      if (value == 0) continue;
      sum += value;
      System.out.println("  " + key + ": " + value);
    }

    // leave some index-space for plugin developers
    assertTrue(IElementType.getAllocatedTypesCount() < 10000);
    assertEquals(IElementType.getAllocatedTypesCount(), sum);

    // output on 11.05.2012
    //   Preloaded: 3485 types
    //   95 definitions
    //   Total: 7694 types
  }

}
