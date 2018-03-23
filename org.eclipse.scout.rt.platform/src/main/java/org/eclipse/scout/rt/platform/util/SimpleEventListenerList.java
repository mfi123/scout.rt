/*******************************************************************************
 * Copyright (c) 2018 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.platform.util;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.WeakHashMap;

/**
 * High performance event listener list with support for frequent add/remove and weak listners.
 * <p>
 * The high performance is reached by setting removed and garbage collected weak listeners to null instead on completely
 * removing them. The rebuild of the internal listener list is done lazy when there are enough accumulated null values.
 *
 * @since 7.1
 */
public class SimpleEventListenerList<LISTENER> implements Iterable<LISTENER> {
  private final List<Object> m_refs = new ArrayList<>();
  private final Map<LISTENER, Integer> m_indexes = new WeakHashMap<>();

  public boolean isEmpty() {
    return m_indexes.isEmpty();
  }

  /**
   * @return the listener count, this may not be the precise count of listeners since null values are lazy removed for
   *         better performance. Also weak references may be cleared at any time.
   */
  public int size() {
    return m_indexes.size();
  }

  /**
   * @return the listener count, this may not be the precise count of listeners since null values are lazy removed for
   *         better performance. Also weak references may be cleared at any time.
   *         <p>
   *         Synonym for {@link #size()}
   */
  public Object getListenerCount() {
    return size();
  }

  public void add(LISTENER listener) {
    add(listener, false);
  }

  public void add(LISTENER listener, boolean weak) {
    if (m_indexes.containsKey(listener)) {
      return;
    }
    Object ref;
    if (weak || listener instanceof WeakEventListener) {
      ref = new WeakReference<LISTENER>(listener);
    }
    else {
      ref = listener;
    }
    int i = m_refs.size();
    m_refs.add(ref);
    m_indexes.put(listener, i);
    maintain();
  }

  public void remove(LISTENER listener) {
    Integer i = m_indexes.remove(listener);
    if (i != null) {
      m_refs.set(i, null);
    }
    maintain();
  }

  /**
   * Iterates all listeners in the order to be called listeners. Null values are skipped automatically.
   */
  @Override
  public Iterator<LISTENER> iterator() {
    maintain();
    return new Iterator<LISTENER>() {
      private int m_index = m_refs.size();
      private LISTENER m_nextValue;

      private LISTENER ensureNext() {
        while (m_nextValue == null && m_index > 0) {
          m_index--;
          m_nextValue = get(m_index);
        }
        return m_nextValue;
      }

      @Override
      public boolean hasNext() {
        return ensureNext() != null;
      }

      @Override
      public LISTENER next() {
        LISTENER val = ensureNext();
        m_nextValue = null;
        if (val == null) {
          throw new NoSuchElementException();
        }
        return val;
      }
    };
  }

  /**
   * listeners are in reverse order, last listener must handle event first
   * <p>
   *
   * @return the listener, potentially null if a weak referenced listener was garbage collected
   */
  @SuppressWarnings("unchecked")
  private LISTENER get(int i) {
    Object ref = m_refs.get(i);
    if (ref instanceof WeakReference) {
      ref = ((Reference) ref).get();
      if (ref == null) {
        m_refs.set(i, null);
      }
    }
    return (LISTENER) ref;
  }

  private void maintain() {
    if (m_indexes.isEmpty()) {
      if (!m_refs.isEmpty()) {
        m_refs.clear();
      }
      return;
    }
    if (m_refs.size() <= 2 * m_indexes.size()) {
      return;
    }
    ArrayList<Object> tmp = new ArrayList<>(m_indexes.size());
    for (int i = 0; i < m_refs.size(); i++) {
      Object ref = m_refs.get(i);
      if (ref instanceof WeakReference) {
        if (((WeakReference) ref).get() != null) {
          tmp.add(ref);
        }
      }
      else if (ref != null) {
        tmp.add(ref);
      }
    }
    m_refs.clear();
    m_indexes.clear();
    if (!tmp.isEmpty()) {
      for (int i = 0; i < tmp.size(); i++) {
        Object ref = tmp.get(i);
        @SuppressWarnings("unchecked")
        LISTENER listener = (LISTENER) (ref instanceof WeakReference ? ((WeakReference) ref).get() : ref);
        m_refs.add(ref);
        m_indexes.put(listener, i);
      }
    }
  }

  /**
   * Used for unit testing
   */
  List<Object> refs() {
    return m_refs;
  }

  /**
   * Used for unit testing
   */
  Map<LISTENER, Integer> indexes() {
    return m_indexes;
  }

}
