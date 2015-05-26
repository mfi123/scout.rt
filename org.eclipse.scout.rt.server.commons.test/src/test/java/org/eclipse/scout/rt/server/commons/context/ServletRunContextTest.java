/*******************************************************************************
 * Copyright (c) 2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.server.commons.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.commons.nls.NlsLocale;
import org.eclipse.scout.rt.platform.job.PropertyMap;
import org.eclipse.scout.rt.server.commons.servlet.IHttpServletRoundtrip;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(PlatformTestRunner.class)
public class ServletRunContextTest {

  @Mock
  private HttpServletRequest m_servletRequest;
  @Mock
  private HttpServletResponse m_servletResponse;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);

    IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST.remove();
    IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE.remove();
  }

  @Test
  public void testEmpty() {
    ServletRunContext runContext = ServletRunContexts.empty();
    assertNull(runContext.servletRequest());
    assertNull(runContext.servletResponse());
  }

  @Test
  public void testCopy() {
    ServletRunContext runContext = ServletRunContexts.empty();
    runContext.propertyMap().put("A", "B");
    runContext.subject(new Subject());
    runContext.locale(Locale.CANADA_FRENCH);
    runContext.servletRequest(m_servletRequest);
    runContext.servletResponse(m_servletResponse);

    ServletRunContext copy = runContext.copy();

    assertEquals(toSet(runContext.propertyMap().iterator()), toSet(copy.propertyMap().iterator()));
    assertSame(runContext.subject(), copy.subject());
    assertSame(runContext.locale(), copy.locale());
    assertSame(m_servletRequest, copy.servletRequest());
    assertSame(m_servletResponse, copy.servletResponse());
  }

  @Test
  public void testCurrentSubject() {
    Subject subject = new Subject();
    ServletRunContext runContext = Subject.doAs(subject, new PrivilegedAction<ServletRunContext>() {

      @Override
      public ServletRunContext run() {
        return ServletRunContexts.copyCurrent();
      }
    });
    assertSame(subject, runContext.subject());

    runContext = Subject.doAs(null, new PrivilegedAction<ServletRunContext>() {

      @Override
      public ServletRunContext run() {
        return ServletRunContexts.copyCurrent();
      }
    });
    assertNull(runContext.subject());
  }

  @Test
  public void testCurrentServletRequest() {
    IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST.remove();
    assertNull(ServletRunContexts.copyCurrent().servletRequest());

    IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST.set(m_servletRequest);
    assertSame(m_servletRequest, ServletRunContexts.copyCurrent().servletRequest());
  }

  @Test
  public void testCurrentServletResponse() {
    IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE.remove();
    assertNull(ServletRunContexts.copyCurrent().servletResponse());

    IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE.set(m_servletResponse);
    assertSame(m_servletResponse, ServletRunContexts.copyCurrent().servletResponse());
  }

  @Test
  public void testCurrentLocale() {
    NlsLocale.CURRENT.set(Locale.CANADA_FRENCH);
    assertEquals(Locale.CANADA_FRENCH, ServletRunContexts.copyCurrent().locale());

    NlsLocale.CURRENT.remove();
    assertNull(ServletRunContexts.copyCurrent().locale());
  }

  @Test
  public void testCurrentPropertyMap() {
    PropertyMap propertyMap = new PropertyMap();
    propertyMap.put("prop", "value");

    // No context on ThreadLocal
    PropertyMap.CURRENT.remove();
    assertNotNull(ServletRunContexts.copyCurrent());
    assertTrue(toSet(ServletRunContexts.copyCurrent().propertyMap().iterator()).isEmpty());

    // Context on ThreadLocal
    PropertyMap.CURRENT.set(propertyMap);
    assertNotSame(propertyMap, ServletRunContexts.copyCurrent().propertyMap()); // test for copy
    assertEquals(toSet(propertyMap.iterator()), toSet(ServletRunContexts.copyCurrent().propertyMap().iterator()));
  }

  private static Set<Object> toSet(Iterator<?> iterator) {
    Set<Object> set = new HashSet<>();
    while (iterator.hasNext()) {
      set.add(iterator.next());
    }
    return set;
  }
}
