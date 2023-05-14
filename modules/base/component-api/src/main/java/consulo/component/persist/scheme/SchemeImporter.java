package consulo.component.persist.scheme;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.internal.RootComponentHolder;
import consulo.component.util.pointer.Named;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Provides functionality to import a scheme from another non-Consulo format.
 *
 * @author Rustam Vishnyakov
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface SchemeImporter<T extends Named> {
  /**
   * Finds extensions supporting the given <code>schemeClass</code>
   *
   * @param schemeClass The class of the scheme to search extensions for.
   * @return A collection of importers capable of importing schemes of the given class. An empty collection is returned if there are
   * no matching importers.
   */
  @Nonnull
  public static <S extends Named> Collection<SchemeImporter<S>> getExtensions(Class<S> schemeClass) {
    List<SchemeImporter<S>> importers = new ArrayList<>();
    for (SchemeImporter<?> schemeImporter : RootComponentHolder.getRootComponent().getExtensionPoint(SchemeImporter.class)) {
      if (schemeClass == schemeImporter.getSchemeClass()) {
        //noinspection unchecked
        importers.add((SchemeImporter<S>)schemeImporter);
      }
    }
    return importers;
  }


  /**
   * Find an importer for the given name and scheme class. It is allowed for importers to have the same name but different scheme classes.
   *
   * @param name        The importer name as defined in plug-in configuration.
   * @param schemeClass The scheme class the importer has to support.
   * @return The found importer or null if there are no importers for the given name and scheme class.
   */
  @Nullable
  public static <S extends Named> SchemeImporter<S> getImporter(@Nonnull String name, Class<S> schemeClass) {
    for (SchemeImporter<S> importer : getExtensions(schemeClass)) {
      if (name.equals(importer.getName())) {
        return importer;
      }
    }
    return null;
  }

  /**
   * Class of scheme impl
   */
  @Nonnull
  Class<T> getSchemeClass();

  @Nonnull
  String getName();

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
