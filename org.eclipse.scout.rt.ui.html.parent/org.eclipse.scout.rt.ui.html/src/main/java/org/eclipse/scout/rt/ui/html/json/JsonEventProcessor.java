/*******************************************************************************
 * Copyright (c) 2010 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
package org.eclipse.scout.rt.ui.html.json;

import org.eclipse.scout.commons.IRunnable;
import org.eclipse.scout.commons.exception.ProcessingException;
import org.eclipse.scout.commons.logger.IScoutLogger;
import org.eclipse.scout.commons.logger.ScoutLogManager;
import org.eclipse.scout.rt.client.IClientSession;
import org.eclipse.scout.rt.client.job.ModelJobInput;
import org.eclipse.scout.rt.client.job.ModelJobs;
import org.eclipse.scout.rt.platform.job.IFuture;
import org.eclipse.scout.rt.ui.html.ModelJobUtility;

/**
 * Processes JSON events from the UI in a Scout client job and waits until all sync jobs have been finished.
 */
public class JsonEventProcessor {

  private static final IScoutLogger LOG = ScoutLogManager.getLogger(JsonEventProcessor.class);

  private final IJsonSession m_jsonSession;

  public JsonEventProcessor(IJsonSession jsonSession) {
    m_jsonSession = jsonSession;
  }

  public void processEvents(final JsonRequest request, final JsonResponse response) {
    IClientSession clientSession = m_jsonSession.getClientSession();
    IFuture<Void> future = ModelJobs.schedule(new IRunnable() {
      @Override
      public void run() throws Exception {
        ModelJobUtility.runAsSubject(new Runnable() {
          @Override
          public void run() {
            for (final JsonEvent event : request.getEvents()) {
              processEvent(event, response);
            }
          }
        });
      }
    }, ModelJobInput.defaults().session(clientSession).name("processEvents"));
    ModelJobUtility.waitUntilAllModelJobsJobsHaveFinished(clientSession);
    try {
      // If model job has finished (= not waitFor), check if there was an exception
      if (future.isDone()) {
        future.awaitDoneAndGet();
      }
    }
    catch (ProcessingException e) {
      throw new JsonException("Error while processing events", e);
    }
  }

  protected void processEvent(JsonEvent event, JsonResponse response) {
    final IJsonAdapter jsonAdapter = m_jsonSession.getJsonAdapter(event.getTarget());
    if (jsonAdapter == null) {
      throw new JsonException("No adapter found for ID " + event.getTarget());
    }
    try {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Handling event '" + event.getType() + "' for adapter with ID " + event.getTarget());
      }
      jsonAdapter.handleUiEvent(event, response);
      jsonAdapter.cleanUpEventFilters();
    }
    catch (Exception t) {
      LOG.error("Error while handling event '" + event.getType() + "' for adapter with ID " + event.getTarget());
      throw new JsonException(t);
    }
  }
}
