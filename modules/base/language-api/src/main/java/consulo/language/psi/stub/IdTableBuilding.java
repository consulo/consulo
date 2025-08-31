/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.language.psi.stub;

import consulo.application.util.function.Processor;
import consulo.language.Language;
import consulo.language.ast.TokenSet;
import consulo.language.cacheBuilder.*;
import consulo.language.file.LanguageFileType;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.internal.custom.CustomFileTypeLexer;
import consulo.language.internal.custom.CustomHighlighterTokenType;
import consulo.language.custom.CustomSyntaxTableFileType;
import consulo.language.psi.search.UsageSearchContext;
import consulo.util.lang.CharArrayUtil;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class IdTableBuilding {
  private IdTableBuilding() {
  }

  public interface ScanWordProcessor {
    void run(CharSequence chars, @Nullable char[] charsArray, int start, int end);
  }

  private static final HashMap<FileType, IdIndexer> ourIdIndexers = new HashMap<>();

  @Deprecated
  public static void registerIdIndexer(FileType fileType, IdIndexer indexer) {
    ourIdIndexers.put(fileType, indexer);
  }

  public static boolean isIdIndexerRegistered(FileType fileType) {
    return ourIdIndexers.containsKey(fileType) || IdIndexer.forFileType(fileType) != null;
  }

  @Nullable
  public static IdIndexer getFileTypeIndexer(FileType fileType) {
    return getFileTypeIndexer(CacheBuilderRegistry.getInstance(), fileType);
  }

  @Nullable
  public static IdIndexer getFileTypeIndexer(CacheBuilderRegistry registry, FileType fileType) {
    IdIndexer idIndexer = ourIdIndexers.get(fileType);

    if (idIndexer != null) {
      return idIndexer;
    }

    IdIndexer extIndexer = IdIndexer.forFileType(fileType);
    if (extIndexer != null) {
      return extIndexer;
    }

    WordsScanner customWordsScanner = registry.getCacheBuilder(fileType);
    if (customWordsScanner != null) {
      return new WordsScannerFileTypeIdIndexerAdapter(customWordsScanner);
    }

    if (fileType instanceof LanguageFileType) {
      Language lang = ((LanguageFileType)fileType).getLanguage();
      FindUsagesProvider findUsagesProvider = FindUsagesProvider.forLanguage(lang);
      WordsScanner scanner = findUsagesProvider.getWordsScanner();
      if (scanner == null) {
        scanner = new SimpleWordsScanner();
      }
      return new WordsScannerFileTypeIdIndexerAdapter(scanner);
    }

    if (fileType instanceof CustomSyntaxTableFileType) {
      return new WordsScannerFileTypeIdIndexerAdapter(createWordScanner((CustomSyntaxTableFileType)fileType));
    }

    return null;
  }

  private static WordsScanner createWordScanner(CustomSyntaxTableFileType customSyntaxTableFileType) {
    return new DefaultWordsScanner(new CustomFileTypeLexer(customSyntaxTableFileType.getSyntaxTable(), true), TokenSet.create(CustomHighlighterTokenType.IDENTIFIER),
                                   TokenSet.create(CustomHighlighterTokenType.LINE_COMMENT, CustomHighlighterTokenType.MULTI_LINE_COMMENT),
                                   TokenSet.create(CustomHighlighterTokenType.STRING, CustomHighlighterTokenType.SINGLE_QUOTED_STRING));

  }

  private static class WordsScannerFileTypeIdIndexerAdapter implements IdIndexer {
    private final WordsScanner myScanner;

    public WordsScannerFileTypeIdIndexerAdapter(@Nonnull WordsScanner scanner) {
      myScanner = scanner;
    }

    @Override
    @Nonnull
    public Map<IdIndexEntry, Integer> map(FileContent inputData) {
      final CharSequence chars = inputData.getContentAsText();
      final char[] charsArray = CharArrayUtil.fromSequenceWithoutCopying(chars);
      final IdDataConsumer consumer = new IdDataConsumer();
      myScanner.processWords(chars, new Processor<WordOccurrence>() {
        @Override
        public boolean process(WordOccurrence t) {
          if (charsArray != null && t.getBaseText() == chars) {
            consumer.addOccurrence(charsArray, t.getStart(), t.getEnd(), convertToMask(t.getKind()));
          }
          else {
            consumer.addOccurrence(t.getBaseText(), t.getStart(), t.getEnd(), convertToMask(t.getKind()));
          }
          return true;
        }

        private int convertToMask(WordOccurrence.Kind kind) {
          if (kind == null) return UsageSearchContext.ANY;
          if (kind == WordOccurrence.Kind.CODE) return UsageSearchContext.IN_CODE;
          if (kind == WordOccurrence.Kind.COMMENTS) return UsageSearchContext.IN_COMMENTS;
          if (kind == WordOccurrence.Kind.LITERALS) return UsageSearchContext.IN_STRINGS;
          if (kind == WordOccurrence.Kind.FOREIGN_LANGUAGE) return UsageSearchContext.IN_FOREIGN_LANGUAGES;
          return 0;
        }
      });
      return consumer.getResult();
    }

    @Nonnull
    @Override
    public FileType getFileType() {
      throw new UnsupportedOperationException();
    }
  }

  public static void scanWords(ScanWordProcessor processor, CharSequence chars, int startOffset, int endOffset) {
    scanWords(processor, chars, CharArrayUtil.fromSequenceWithoutCopying(chars), startOffset, endOffset, false);
  }

  public static void scanWords(ScanWordProcessor processor,
                               CharSequence chars,
                               @Nullable char[] charArray,
                               int startOffset,
                               int endOffset,
                               boolean mayHaveEscapes) {
    int index = startOffset;
    boolean hasArray = charArray != null;

    ScanWordsLoop:
    while (true) {
      while (true) {
        if (index >= endOffset) break ScanWordsLoop;
        char c = hasArray ? charArray[index] : chars.charAt(index);

        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (Character.isJavaIdentifierStart(c) && c != '$')) {
          break;
        }
        index++;
        if (mayHaveEscapes && c == '\\') index++; //the next symbol is for escaping
      }
      int index1 = index;
      while (true) {
        index++;
        if (index >= endOffset) break;
        char c = hasArray ? charArray[index] : chars.charAt(index);
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) continue;
        if (!Character.isJavaIdentifierPart(c) || c == '$') break;
      }
      if (index - index1 > 100) continue; // Strange limit but we should have some!

      processor.run(chars, charArray, index1, index);
    }
  }
}
