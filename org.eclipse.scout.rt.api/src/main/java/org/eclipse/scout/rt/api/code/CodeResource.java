/*
 * Copyright (c) 2010, 2024 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.scout.rt.api.code;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.scout.rt.api.data.code.CodeDo;
import org.eclipse.scout.rt.api.data.code.CodeTypeDo;
import org.eclipse.scout.rt.api.data.code.IApiExposedCodeTypeContributor;
import org.eclipse.scout.rt.platform.BEANS;
import org.eclipse.scout.rt.platform.context.RunContexts;
import org.eclipse.scout.rt.platform.nls.NlsLocale;
import org.eclipse.scout.rt.platform.util.CollectionUtility;
import org.eclipse.scout.rt.platform.util.ObjectUtility;
import org.eclipse.scout.rt.platform.util.StringUtility;
import org.eclipse.scout.rt.rest.IRestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("codes")
public class CodeResource implements IRestResource {

  private static final Logger LOG = LoggerFactory.getLogger(CodeResource.class);

  /**
   * Gets all CodeTypes which should be published to the UI on application startup (bootstrap)
   *
   * @param allLanguages
   *          {@code true} if all application languages should be exported. {@code false} if only the texts for the
   *          current {@link NlsLocale} should be part of the response. Customize {@link #getApplicationLanguages()} to
   *          specify the supported application languages.
   * @return List of the CodeTypes.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<CodeTypeDo> list(@QueryParam("allLanguages") @DefaultValue("true") boolean allLanguages) {
    Map<String, CodeTypeDo> codeTypeDos = getCodeTypesById();
    if (allLanguages) {
      loadOtherTextsFor(codeTypeDos);
    }
    return codeTypeDos.values();
  }

  protected Map<String, CodeTypeDo> getCodeTypesById() {
    Set<CodeTypeDo> codeTypes = new HashSet<>();
    BEANS.all(IApiExposedCodeTypeContributor.class).forEach(contributor -> contributor.contribute(codeTypes));
    return convertToMap(codeTypes);
  }

  protected Map<String, CodeTypeDo> convertToMap(Collection<CodeTypeDo> codeTypes) {
    Map<String, CodeTypeDo> result = new HashMap<>(codeTypes.size());
    codeTypes.forEach(codeTypeDo -> putToMap(codeTypeDo, result));
    return result;
  }

  protected void putToMap(CodeTypeDo codeType, Map<String, CodeTypeDo> map) {
    if (codeType == null) {
      return;
    }
    String id = codeType.getId();
    if (!StringUtility.hasText(id)) {
      LOG.warn("Skipping CodeType without id.");
      return; // skip CodeType without id
    }
    CodeTypeDo previous = map.put(id, codeType);
    if (previous != null) {
      LOG.warn("CodeType with duplicate id '{}' found.", id);
    }
  }

  protected void loadOtherTextsFor(Map<String, CodeTypeDo> codeTypeMap) {
    Set<Locale> otherAppLanguages = new HashSet<>(getApplicationLanguages());
    otherAppLanguages.remove(NlsLocale.get()); // remove current running locale (is already part of the result)
    if (otherAppLanguages.isEmpty()) {
      return; // no other languages available
    }

    otherAppLanguages.stream()
        // order by language tag, locales without location come first ([de, en_US, de_DE, en, en_GB] -> [de, de_DE, en, en_GB, en_US])
        .sorted(Comparator.comparing(Locale::toLanguageTag))
        .forEachOrdered(otherLanguage -> RunContexts.copyCurrent()
            .withLocale(otherLanguage)
            .call(this::getCodeTypesById).values()
            .forEach(otherCodeTypeDo -> mergeCodeTypeTexts(otherCodeTypeDo, codeTypeMap)));
  }

  protected void mergeCodeTypeTexts(CodeTypeDo from, Map<String, CodeTypeDo> targetMap) {
    if (from == null) {
      return;
    }
    CodeTypeDo target = targetMap.get(from.getId());
    if (target == null) {
      return;
    }
    mergeTexts(from.getTexts(), target.getTexts(), target::withText);
    mergeTexts(from.getTextsPlural(), target.getTextsPlural(), target::withTextPlural);
    Collection<CodeDo> targetChildren = getChildCodes(target);
    from.getCodes().forEach(code -> mergeCodeTexts(code, targetChildren));
  }

  protected void mergeCodeTexts(CodeDo from, Collection<CodeDo> targetList) {
    if (from == null) {
      return;
    }
    Object codeId = from.getId();
    CodeDo target = targetList.stream()
        .filter(code -> Objects.equals(code.getId(), codeId))
        .findAny().orElse(null);
    if (target == null) {
      return;
    }
    mergeTexts(from.getTexts(), target.getTexts(), target::withText);
    Collection<CodeDo> targetChildren = getChildCodes(target);
    from.getChildren().forEach(code -> mergeCodeTexts(code, targetChildren));
  }

  /**
   * Merges the to text maps (languageTag to text). If there is already an entry in the target map with the same text
   * and a languageTag that is a prefix of the entry in the from map, this entry is skipped as this text is considered
   * to be inherited. <br>
   * Examples:
   * <ul>
   * <li>merge {de_DE=groß} into {de=groß} => {de=groß}</li>
   * <li>merge {de_CH=gross} into {de=groß} => {de=groß, de_CH=gross}</li>
   * <li>merge {de=ok} into {en=ok} => {de=ok, en=ok}</li>
   * </ul>
   */
  protected void mergeTexts(Map<String /* languageTag */, String /* text */> fromTexts, Map<String /* languageTag */, String /* text */> targetTexts, BiConsumer<String /* languageTag */, String /* text */> withText) {
    if (CollectionUtility.isEmpty(fromTexts)) {
      // no fromTexts -> nothing to merge
      return;
    }

    if (CollectionUtility.isEmpty(targetTexts)) {
      // no targetTexts -> copy everything
      fromTexts.forEach(withText);
      return;
    }

    fromTexts.entrySet()
        .stream()
        .filter(entry -> targetTexts.entrySet().stream()
            // check if there is already an entry with a languageTag that is a prefix to the current one ...
            .noneMatch(targetEntry -> StringUtility.startsWith(entry.getKey(), targetEntry.getKey())
                // ... and the same text
                && ObjectUtility.equals(entry.getValue(), targetEntry.getValue())))
        .forEach(entry -> withText.accept(entry.getKey(), entry.getValue()));
  }

  protected Collection<CodeDo> getChildCodes(CodeTypeDo parent) {
    return parent.getCodes();
  }

  protected Collection<CodeDo> getChildCodes(CodeDo parent) {
    return parent.getChildren();
  }

  protected Collection<Locale> getApplicationLanguages() {
    return Collections.singleton(NlsLocale.get());
  }
}
