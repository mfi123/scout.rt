/*******************************************************************************
 * Copyright (c) 2010-2015 BSI Business Systems Integration AG.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Iterator;

import org.eclipse.scout.rt.platform.chain.IChainable;
import org.eclipse.scout.rt.platform.chain.callable.CallableChain;
import org.eclipse.scout.rt.platform.context.CorrelationId;
import org.eclipse.scout.rt.platform.context.PropertyMap;
import org.eclipse.scout.rt.platform.context.RunContext;
import org.eclipse.scout.rt.platform.context.RunContextIdentifiers;
import org.eclipse.scout.rt.platform.context.RunMonitor;
import org.eclipse.scout.rt.platform.logger.DiagnosticContextValueProcessor;
import org.eclipse.scout.rt.platform.nls.NlsLocale;
import org.eclipse.scout.rt.platform.security.SubjectProcessor;
import org.eclipse.scout.rt.platform.transaction.TransactionProcessor;
import org.eclipse.scout.rt.platform.util.ThreadLocalProcessor;
import org.eclipse.scout.rt.server.commons.servlet.IHttpServletRoundtrip;
import org.eclipse.scout.rt.testing.platform.runner.PlatformTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PlatformTestRunner.class)
public class ServletRunContextChainTest {

  /**
   * Tests the correct order of interceptors in {@link ServletRunContext}.
   */
  @Test
  public void testCallableChain() throws Exception {
    CallableChain<Object> chain = new ServletRunContext() {
      @Override
      protected <RESULT> CallableChain<RESULT> createCallableChain() { // overwrite to be accessible in test
        return super.createCallableChain();
      }
    }.createCallableChain();

    Iterator<IChainable> chainIterator = chain.values().iterator();

    // 1. ThreadLocalProcessor for RunContext.CURRENT
    IChainable c = chainIterator.next();
    assertEquals(ThreadLocalProcessor.class, c.getClass());
    assertSame(RunContext.CURRENT, ((ThreadLocalProcessor) c).getThreadLocal());

    // 2. ThreadLocalProcessor for CorrelationId.CURRENT
    c = chainIterator.next();
    assertEquals(ThreadLocalProcessor.class, c.getClass());
    assertSame(CorrelationId.CURRENT, ((ThreadLocalProcessor) c).getThreadLocal());

    // 3. ThreadLocalProcessor for RunMonitor.CURRENT
    c = chainIterator.next();
    assertEquals(ThreadLocalProcessor.class, c.getClass());
    assertSame(RunMonitor.CURRENT, ((ThreadLocalProcessor) c).getThreadLocal());

    // 4. SubjectProcessor
    c = (IChainable) chainIterator.next();
    assertEquals(SubjectProcessor.class, c.getClass());

    // 5. DiagnosticContextValueProcessor
    c = chainIterator.next();
    assertEquals(DiagnosticContextValueProcessor.class, c.getClass());
    assertEquals("subject.principal.name", ((DiagnosticContextValueProcessor) c).getMdcKey());

    // 6. DiagnosticContextValueProcessor
    c = chainIterator.next();
    assertEquals(DiagnosticContextValueProcessor.class, c.getClass());
    assertEquals("scout.correlation.id", ((DiagnosticContextValueProcessor) c).getMdcKey());

    // 7. ThreadLocalProcessor for NlsLocale.CURRENT
    c = chainIterator.next();
    assertEquals(ThreadLocalProcessor.class, c.getClass());
    assertSame(NlsLocale.CURRENT, ((ThreadLocalProcessor) c).getThreadLocal());

    // 8. ThreadLocalProcessor for PropertyMap.CURRENT
    c = chainIterator.next();
    assertEquals(ThreadLocalProcessor.class, c.getClass());
    assertSame(PropertyMap.CURRENT, ((ThreadLocalProcessor) c).getThreadLocal());

    // 9. ThreadLocalProcessor for RunContextIdentifiers.CURRENT
    c = chainIterator.next();
    assertEquals(ThreadLocalProcessor.class, c.getClass());
    assertSame(RunContextIdentifiers.CURRENT, ((ThreadLocalProcessor) c).getThreadLocal());

    // 10. ThreadLocalProcessor for IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST
    c = chainIterator.next();
    assertEquals(ThreadLocalProcessor.class, c.getClass());
    assertSame(IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST, ((ThreadLocalProcessor) c).getThreadLocal());

    // 11. ThreadLocalProcessor for IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE
    c = chainIterator.next();
    assertEquals(ThreadLocalProcessor.class, c.getClass());
    assertSame(IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE, ((ThreadLocalProcessor) c).getThreadLocal());

    // 12. DiagnosticContextValueProcessor
    c = chainIterator.next();
    assertEquals(DiagnosticContextValueProcessor.class, c.getClass());
    assertEquals("http.session.id", ((DiagnosticContextValueProcessor) c).getMdcKey());

    // 13. DiagnosticContextValueProcessor
    c = chainIterator.next();
    assertEquals(DiagnosticContextValueProcessor.class, c.getClass());
    assertEquals("http.request.uri", ((DiagnosticContextValueProcessor) c).getMdcKey());

    // 14. DiagnosticContextValueProcessor
    c = chainIterator.next();
    assertEquals(DiagnosticContextValueProcessor.class, c.getClass());
    assertEquals("http.request.method", ((DiagnosticContextValueProcessor) c).getMdcKey());

    // 15. DiagnosticContextValueProcessor
    c = chainIterator.next();
    assertEquals(DiagnosticContextValueProcessor.class, c.getClass());
    assertEquals("http.request.querystring", ((DiagnosticContextValueProcessor) c).getMdcKey());

    // 16. TransactionProcessor
    c = chainIterator.next();
    assertEquals(TransactionProcessor.class, c.getClass());
    assertFalse(chainIterator.hasNext());
  }
}
