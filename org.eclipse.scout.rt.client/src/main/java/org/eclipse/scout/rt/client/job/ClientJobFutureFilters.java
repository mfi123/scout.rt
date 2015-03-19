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
package org.eclipse.scout.rt.client.job;

import java.util.Collection;

import org.eclipse.scout.commons.filter.IFilter;
import org.eclipse.scout.commons.filter.NotFilter;
import org.eclipse.scout.commons.filter.OrFilter;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.platform.job.JobInput;
import org.eclipse.scout.rt.shared.ISession;

/**
 * Factory methods to create Future filters related to client and model jobs.
 *
 * @since 5.1
 */
public final class ClientJobFutureFilters {

  private ClientJobFutureFilters() {
  }

  /**
   * Creates a filter to accept only client- or model jobs with some specific characteristics. The filter is designed to
   * support method chaining.
   */
  public static Filter newFilter() {
    return new Filter();
  }

  /**
   * Filter to accept only client- or model jobs with the given characteristics.
   * <p/>
   * The 'setter-methods' returns <code>this</code> in order to support for method chaining.
   *
   * @since 5.1
   */
  public static class Filter extends org.eclipse.scout.rt.platform.job.JobFutureFilters.Filter {

    @Override
    protected void postConstruct() {
      m_filters.add(new OrFilter<>(ClientJobFilter.INSTANCE, ModelJobFilter.INSTANCE));
    }

    @Override
    public Filter id(String id) {
      return (Filter) super.id(id);
    }

    @Override
    public Filter futures(final IFuture<?>... futures) {
      return (Filter) super.futures(futures);
    }

    @Override
    public Filter futures(final Collection<IFuture<?>> futures) {
      return (Filter) super.futures(futures);
    }

    @Override
    public Filter currentFuture() {
      return (Filter) super.currentFuture();
    }

    @Override
    public Filter notCurrentFuture() {
      return (Filter) super.notCurrentFuture();
    }

    @Override
    public Filter blocked() {
      return (Filter) super.blocked();
    }

    @Override
    public Filter notBlocked() {
      return (Filter) super.notBlocked();
    }

    @Override
    public Filter periodic() {
      return (Filter) super.periodic();
    }

    @Override
    public Filter notPeriodic() {
      return (Filter) super.notPeriodic();
    }

    /**
     * To accept only jobs which are run on behalf of the given client session.
     */
    public Filter session(final IClientSession session) {
      m_filters.add(new SessionFilter(session));
      return this;
    }

    /**
     * To accept only jobs which are run on behalf of the current client session.
     *
     * @see ISession#CURRENT
     */
    public Filter currentSession() {
      m_filters.add(new SessionFilter(ISession.CURRENT.get()));
      return this;
    }

    /**
     * To accept only jobs which are not run on behalf of the current client session.
     *
     * @see ISession#CURRENT
     */
    public Filter notCurrentSession() {
      m_filters.add(new NotFilter<>(new SessionFilter(ISession.CURRENT.get())));
      return this;
    }

    /**
     * To accept only model jobs, and not client jobs.
     */
    public Filter modelJobsOnly() {
      m_filters.add(ModelJobFilter.INSTANCE);
      return this;
    }

    /**
     * To accept only client jobs, and not model jobs.
     */
    public Filter clientJobsOnly() {
      m_filters.add(ClientJobFilter.INSTANCE);
      return this;
    }
  }

  /**
   * Filter which accepts Futures only if belonging to the given client session.
   *
   * @since 5.1
   */
  public static class SessionFilter implements IFilter<IFuture<?>> {

    private final ISession m_session;

    public SessionFilter(final ISession session) {
      m_session = session;
    }

    @Override
    public boolean accept(final IFuture<?> future) {
      final JobInput jobInput = future.getJobInput();
      if (jobInput instanceof ClientJobInput) {
        return (m_session == ((ClientJobInput) jobInput).getSession());
      }
      else {
        return false;
      }
    }
  }

  /**
   * Filter which accepts only Futures from client jobs.
   *
   * @since 5.1
   */
  public static class ClientJobFilter implements IFilter<IFuture<?>> {

    public static final IFilter<IFuture<?>> INSTANCE = new ClientJobFilter();

    private ClientJobFilter() {
    }

    @Override
    public boolean accept(final IFuture<?> future) {
      return ClientJobs.isClientJob(future);
    }
  }

  /**
   * Filter which accepts only Futures from model jobs.
   *
   * @since 5.1
   */
  public static class ModelJobFilter implements IFilter<IFuture<?>> {

    public static final IFilter<IFuture<?>> INSTANCE = new ModelJobFilter();

    private ModelJobFilter() {
    }

    @Override
    public boolean accept(final IFuture<?> future) {
      return ModelJobs.isModelJob(future);
    }
  }
}
