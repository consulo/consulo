package org.mustbe.consulo.roots;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author VISTALL
 * @since 13:23/07.11.13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ContentFoldersSupport {
  Class<? extends ContentFolderTypeProvider>[] value();
}
