/*****************************************************************************
 * Copyright (c) PicoContainer Organization. All rights reserved.            *
 * ------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the BSD      *
 * style license a copy of which has been included with this distribution in *
 * the LICENSE.txt file.                                                     *
 *                                                                           *
 * Idea by Rachel Davies, Original code by Aslak Hellesoy and Paul Hammant   *
 *****************************************************************************/

package consulo.component.impl.internal.inject;

/**
 * @author Jon Tirs&eacute;n
 * @version $Revision: 1.5 $
 */
class DuplicateComponentKeyRegistrationException extends PicoRegistrationException {
  private Object key;

  public DuplicateComponentKeyRegistrationException(Object key) {
    super("Key " + key + " duplicated");
    this.key = key;
  }

  public Object getDuplicateKey() {
    return key;
  }
}
