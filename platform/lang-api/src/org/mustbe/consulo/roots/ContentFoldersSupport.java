package org.mustbe.consulo.roots;

import org.mustbe.consulo.DeprecationInfo;

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
@Deprecated
@DeprecationInfo(value = "Use 'org.mustbe.consulo.roots.ContentFolderSupportPatcher'", until = "1.0")
public @interface ContentFoldersSupport {
  Class<? extends ContentFolderTypeProvider>[] value();
}
