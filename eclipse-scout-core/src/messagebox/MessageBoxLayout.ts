/*
 * Copyright (c) 2010, 2024 BSI Business Systems Integration AG
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import {AbstractLayout, DialogLayout, graphics, HtmlComponent, MessageBox, scrollbars} from '../index';

export class MessageBoxLayout extends AbstractLayout {
  messageBox: MessageBox;

  constructor(messageBox: MessageBox) {
    super();
    this.messageBox = messageBox;
  }

  override layout($container: JQuery) {
    let htmlComp = HtmlComponent.get($container),
      windowSize = $container.windowSize();

    this.messageBox.$container.css('max-width', Number.MAX_VALUE);
    let buttonsSize = graphics.prefSize(this.messageBox.$buttons, {
      exact: true
    }); // TODO CGU what happens on small screens if there is not enough space?
    this.messageBox.$container.css('max-width', '');
    this.messageBox.$content.css('height', 'calc(100% - ' + buttonsSize.height + 'px)');
    this.messageBox.$container.css('--min-width', buttonsSize.width + 'px');

    let currentBounds = htmlComp.offsetBounds(true);
    let messageBoxSize = htmlComp.size();
    let messageBoxMargins = htmlComp.margins();

    messageBoxSize = DialogLayout.fitContainerInWindow(windowSize, currentBounds.point(), messageBoxSize, messageBoxMargins);

    // Add markers to be able to style the msg box in a different way when it uses the full width or height
    $container
      .toggleClass('full-width', (currentBounds.x === 0 && messageBoxMargins.horizontal() === 0 && windowSize.width === messageBoxSize.width))
      .toggleClass('full-height', (currentBounds.y === 0 && messageBoxMargins.vertical() === 0 && windowSize.height === messageBoxSize.height));

    graphics.setSize($container, messageBoxSize);

    scrollbars.update(this.messageBox.$content);

    $container.cssPosition(DialogLayout.positionContainerInWindow($container));
  }
}
