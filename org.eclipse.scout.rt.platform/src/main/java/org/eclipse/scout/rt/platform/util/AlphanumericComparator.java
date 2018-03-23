/*******************************************************************************
 * Copyright (c) 2010-2017 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.platform.util;

import java.io.Serializable;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An implementation of {@link Comparator} that splits strings up in numeric and non-numeric parts and compares each of
 * them individually with the appropriate method.
 * <p>
 * Example:
 *
 * <pre>
 * Unsorted       Default string sort    Alphanumeric sort
 * --------------------------------------------------------
 * myfile.doc     doc10.txt              doc8
 * doc10.txt      doc8                   doc9.txt
 * doc9.txt       doc9.txt               doc10.txt
 * doc8           myfile.doc             myfile.txt
 * </pre>
 *
 * @since 5.2
 */
public class AlphanumericComparator implements Comparator<String>, Serializable {

  private static final long serialVersionUID = 1L;
  private final boolean m_ignoreCase;

  /**
   * Creates a new alphanumeric comparator with {@code ignoreCase = false}.
   */
  public AlphanumericComparator() {
    this(false);
  }

  /**
   * Creates a new alphanumeric comparator.
   *
   * @param ignoreCase
   *          whether the case should be ignored when comparing strings
   */
  public AlphanumericComparator(boolean ignoreCase) {
    m_ignoreCase = ignoreCase;
  }

  public boolean isIgnoreCase() {
    return m_ignoreCase;
  }

  private static final Pattern NUN_OR_TEXT_PATTERN = Pattern.compile("(?:([0-9]+)|([^0-9]+))", Pattern.DOTALL);

  @Override
  public int compare(String s1, String s2) {
    if (s1 == null && s2 == null) {
      return 0;
    }
    if (s1 == null) {
      return -1;
    }
    if (s2 == null) {
      return 1;
    }
    Matcher m1 = NUN_OR_TEXT_PATTERN.matcher(s1);
    Matcher m2 = NUN_OR_TEXT_PATTERN.matcher(s2);
    boolean found1 = m1.find();
    boolean found2 = m2.find();
    int result;
    while (found1 && found2) {
      if (m1.group(1) != null && m2.group(1) != null) {
        result = compareAsLongs(Long.parseLong(m1.group(1)), Long.parseLong(m2.group(1)));
      }
      else {
        result = compareAsStrings(m1.group(2), m2.group(2));
      }
      if (result != 0) {
        return result;
      }
      found1 = m1.find();
      found2 = m2.find();
    }
    return compareFound(found1, found2);
  }

  /**
   * Compares the two strings {@code s1} and {@code s2} as {@link Long}s if possible. If not, a string comparison is
   * done.
   */
  protected int compareAsLongs(Long n1, Long n2) {
    return n1.compareTo(n2);
  }

  /**
   * Compares the two strings {@code s1} and {@code s2}.
   */
  protected int compareAsStrings(String s1, String s2) {
    return (isIgnoreCase() ? StringUtility.compareIgnoreCase(s1, s2) : StringUtility.compare(s1, s2));
  }

  /**
   * Compares the {@link Matcher}s {@code m1} and {@code m2} by checking if ends have been hit.
   */
  protected int compareFound(boolean f1, boolean f2) {
    if (!f1 && f2) {
      // s1 has more parts
      return -1;
    }
    else if (f1 && !f2) {
      // s2 has more parts
      return 1;
    }
    // Both are the same
    return 0;
  }
}
