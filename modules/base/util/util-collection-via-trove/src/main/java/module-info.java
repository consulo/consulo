import org.jspecify.annotations.NullMarked;

/**
 * @author VISTALL
 * @since 2022-02-04
 */
@NullMarked
module consulo.util.collection.via.trove {
  requires static consulo.annotation;
  requires transitive consulo.util.collection;
  requires transitive consulo.util.collection.primitive;

  requires gnu.trove;

  provides consulo.util.collection.impl.CollectionFactory with consulo.util.collection.trove.impl.TroveCollectionFactory;
}