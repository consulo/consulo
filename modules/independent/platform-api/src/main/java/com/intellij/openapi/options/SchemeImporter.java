package com.intellij.openapi.options;

import consulo.util.pointers.Named;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.InputStream;

/**
 * Provides functionality to import a scheme from another non-IntelliJ IDEA format.
 *
 * @author Rustam Vishnyakov
 */
public interface SchemeImporter <T extends Named> {

  /**
   * @return An extension of a source file which can be imported, for example, "xml".
   */
  String getSourceExtension();

  /**
   * Attempts to read scheme names from the given stream. The stream may contain several schemes in which case all the available
   * names are returned.
   *
   * @param inputStream The input stream to read the name from.
   * @return Either scheme name or null if the scheme doesn't have a name.
   * @throws SchemeImportException
   */
  @Nonnull
  String[] readSchemeNames(@Nonnull InputStream inputStream) throws SchemeImportException;

  /**
   * Import a scheme from the given stream and source scheme name.
   *
   * @param inputStream  The input stream to import from.
   * @param sourceScheme The source scheme name (one of returned by <code>readSchemeNames</code> method).
   * @param scheme       The target scheme receiving data.
   */
  void importScheme(@Nonnull InputStream inputStream, @Nullable String sourceScheme, T scheme) throws SchemeImportException;
}
