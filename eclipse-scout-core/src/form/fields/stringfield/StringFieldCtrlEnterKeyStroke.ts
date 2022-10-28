/*
 * Copyright (c) 2010-2022 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 */
import {keys, KeyStroke, ScoutKeyboardEvent, StringField} from '../../../index';

export default class StringFieldCtrlEnterKeyStroke extends KeyStroke {
  declare field: StringField;

  constructor(stringField: StringField) {
    super();
    this.field = stringField;
    this.which = [keys.ENTER];
    this.ctrl = true;
  }

  protected override _accept(event: ScoutKeyboardEvent): boolean {
    let accepted = super._accept(event);
    return accepted && this.field.hasAction;
  }

  override handle(event: JQuery.KeyboardEventBase) {
    this.field._onIconClick();
  }
}
