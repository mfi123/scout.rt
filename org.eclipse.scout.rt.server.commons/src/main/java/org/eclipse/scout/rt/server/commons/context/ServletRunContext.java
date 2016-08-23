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

import java.util.Locale;
import java.util.Map;

import javax.security.auth.Subject;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.chain.callable.CallableChain;
import org.eclipse.scout.rt.platform.context.RunContext;
import org.eclipse.scout.rt.platform.context.RunMonitor;
import org.eclipse.scout.rt.platform.logger.DiagnosticContextValueProcessor;
import org.eclipse.scout.rt.platform.transaction.ITransaction;
import org.eclipse.scout.rt.platform.transaction.ITransactionMember;
import org.eclipse.scout.rt.platform.transaction.TransactionScope;
import org.eclipse.scout.rt.platform.util.ThreadLocalProcessor;
import org.eclipse.scout.rt.platform.util.ToStringBuilder;
import org.eclipse.scout.rt.server.commons.servlet.IHttpServletRoundtrip;
import org.eclipse.scout.rt.server.commons.servlet.logging.HttpRequestMethodContextValueProvider;
import org.eclipse.scout.rt.server.commons.servlet.logging.HttpRequestQueryStringContextValueProvider;
import org.eclipse.scout.rt.server.commons.servlet.logging.HttpRequestUriContextValueProvider;
import org.eclipse.scout.rt.server.commons.servlet.logging.HttpSessionIdContextValueProvider;

/**
 * The <code>ServletRunContext</code> facilitates propagation of the {@link Servlet} state. This context is not intended
 * to be propagated across different threads.
 * <p>
 * A context typically represents a "snapshot" of the current calling state. This class facilitates propagation of that
 * state.
 *
 * @since 5.1
 * @see RunContext
 */
public class ServletRunContext extends RunContext {

  protected HttpServletRequest m_servletRequest;
  protected HttpServletResponse m_servletResponse;

  @Override
  protected <RESULT> void interceptCallableChain(CallableChain<RESULT> callableChain) {
    callableChain
        .add(new ThreadLocalProcessor<>(IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_REQUEST, m_servletRequest))
        .add(new ThreadLocalProcessor<>(IHttpServletRoundtrip.CURRENT_HTTP_SERVLET_RESPONSE, m_servletResponse))
        .add(new DiagnosticContextValueProcessor(BEANS.get(HttpSessionIdContextValueProvider.class)))
        .add(new DiagnosticContextValueProcessor(BEANS.get(HttpRequestUriContextValueProvider.class)))
        .add(new DiagnosticContextValueProcessor(BEANS.get(HttpRequestMethodContextValueProvider.class)))
        .add(new DiagnosticContextValueProcessor(BEANS.get(HttpRequestQueryStringContextValueProvider.class)));
  }

  @Override
  public ServletRunContext withRunMonitor(final RunMonitor runMonitor) {
    super.withRunMonitor(runMonitor);
    return this;
  }

  @Override
  public ServletRunContext withSubject(final Subject subject) {
    super.withSubject(subject);
    return this;
  }

  @Override
  public ServletRunContext withLocale(final Locale locale) {
    super.withLocale(locale);
    return this;
  }

  @Override
  public ServletRunContext withCorrelationId(final String correlationId) {
    super.withCorrelationId(correlationId);
    return this;
  }

  @Override
  public ServletRunContext withTransactionScope(final TransactionScope transactionScope) {
    super.withTransactionScope(transactionScope);
    return this;
  }

  @Override
  public ServletRunContext withTransaction(final ITransaction transaction) {
    super.withTransaction(transaction);
    return this;
  }

  @Override
  public ServletRunContext withTransactionMember(final ITransactionMember transactionMember) {
    super.withTransactionMember(transactionMember);
    return this;
  }

  @Override
  public ServletRunContext withoutTransactionMembers() {
    super.withoutTransactionMembers();
    return this;
  }

  @Override
  public <THREAD_LOCAL> ServletRunContext withThreadLocal(final ThreadLocal<THREAD_LOCAL> threadLocal, final THREAD_LOCAL value) {
    super.withThreadLocal(threadLocal, value);
    return this;
  }

  @Override
  public ServletRunContext withProperty(final Object key, final Object value) {
    super.withProperty(key, value);
    return this;
  }

  @Override
  public ServletRunContext withProperties(final Map<?, ?> properties) {
    super.withProperties(properties);
    return this;
  }

  @Override
  public ServletRunContext withIdentifier(String id) {
    super.withIdentifier(id);
    return this;
  }

  public HttpServletRequest getServletRequest() {
    return m_servletRequest;
  }

  public ServletRunContext withServletRequest(final HttpServletRequest servletRequest) {
    m_servletRequest = servletRequest;
    return this;
  }

  public HttpServletResponse getServletResponse() {
    return m_servletResponse;
  }

  public ServletRunContext withServletResponse(final HttpServletResponse servletResponse) {
    m_servletResponse = servletResponse;
    return this;
  }

  @Override
  protected void interceptToStringBuilder(final ToStringBuilder builder) {
    super.interceptToStringBuilder(builder
        .ref("servletRequest", getServletRequest())
        .ref("servletResponse", getServletResponse()));
  }

  @Override
  protected void copyValues(final RunContext origin) {
    super.copyValues(origin);

    if (origin instanceof ServletRunContext) {
      final ServletRunContext source = (ServletRunContext) origin;
      m_servletRequest = source.m_servletRequest;
      m_servletResponse = source.m_servletResponse;
    }
  }

  @Override
  public ServletRunContext copy() {
    final ServletRunContext copy = BEANS.get(ServletRunContext.class);
    copy.copyValues(this);
    return copy;
  }
}
