/*******************************************************************************
 * Copyright (c) 2014-2015 BSI Business Systems Integration AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     BSI Business Systems Integration AG - initial API and implementation
 ******************************************************************************/
scout.scrollbars = {

  /**
   * Static function to install a scrollbar on a container.
   * When the client supports pretty native scrollbars, we use them by default.
   * Otherwise we install JS-based scrollbars. In that case the install function
   * creates a new scrollbar.js. For native scrollbars we
   * must set some additional CSS styles.
   *
   * @memberOf scout.scrollbars
   */

  _$scrollables: {},

  getScrollables: function(session) {
    // return scrollables for given session
    if (session) {
      return this._$scrollables[session] || [];
    }

    // return all scrollables, no matter to which session they belong
    var $scrollables = [];
    scout.objects.values(this._$scrollables).forEach(function($scrollablesPerSession) {
      scout.arrays.pushAll($scrollables, $scrollablesPerSession);
    });
    return $scrollables;
  },

  pushScrollable: function(session, $container) {
    if (this._$scrollables[session]) {
      if (this._$scrollables[session].indexOf($container) > -1) {
        // already pushed
        return;
      }
      this._$scrollables[session].push($container);
    } else {
      this._$scrollables[session] = [$container];
    }
    $.log.trace('Scrollable added: ' + $container.attr('class') + '. New length: ' + this._$scrollables.length);
  },

  removeScrollable: function(session, $container) {
    var initLength = 0;
    if (this._$scrollables[session]) {
      initLength = this._$scrollables[session].length;
      scout.arrays.$remove(this._$scrollables[session], $container);
      $.log.trace('Scrollable removed: ' + $container.attr('class') + '. New length: ' + this._$scrollables.length);
      if (initLength === this._$scrollables[session].length) {
        throw new Error('scrollable could not be removed. Potential memory leak. ' + $container.attr('class'));
      }
    } else {
      throw new Error('scrollable could not be removed. Potential memory leak. ' + $container.attr('class'));
    }
  },

  install: function($container, options) {
    var scrollbars, scrollbar, nativeScrollbars,
      htmlContainer = scout.HtmlComponent.optGet($container),
      session = options.session || options.parent.session;

    options = options || {};
    options.axis = options.axis || 'both';

    if (options.nativeScrollbars !== undefined) {
      nativeScrollbars = options.nativeScrollbars;
    } else {
      nativeScrollbars = scout.device.hasPrettyScrollbars();
    }
    if (nativeScrollbars) {
      if (scout.device.isIos()) {
        // On ios, container sometimes is not scrollable when installing too early
        // Happens often with nested scrollable containers (e.g. scrollable table inside a form inside a scrollable tree data)
        setTimeout(installNativeScrollbars);
      } else {
        installNativeScrollbars();
      }
    } else {
      installJsScrollbars();
    }
    if (htmlContainer) {
      htmlContainer.scrollable = true;
    }
    $container.data('scrollable', true);
    this.pushScrollable(session, $container);
    return $container;

    function installNativeScrollbars() {
      $.log.trace('use native scrollbars for container ' + scout.graphics.debugOutput($container));
      if (options.axis === 'x') {
        $container
          .css('overflow-x', 'auto')
          .css('overflow-y', 'hidden');
      } else if (options.axis === 'y') {
        $container
          .css('overflow-x', 'hidden')
          .css('overflow-y', 'auto');
      } else {
        $container.css('overflow', 'auto');
      }
      $container.css('-webkit-overflow-scrolling', 'touch');
    }

    function installJsScrollbars() {
      $.log.trace('installing JS-scrollbars for container ' + scout.graphics.debugOutput($container));
      scrollbars = scout.arrays.ensure($container.data('scrollbars'));
      scrollbars.forEach(function(scrollbar) {
        scrollbar.destroy();
      });
      scrollbars = [];
      if (options.axis === 'both') {
        var scrollOptions = $.extend({}, options);
        scrollOptions.axis = 'y';
        scrollbar = scout.create('Scrollbar', scrollOptions);
        scrollbars.push(scrollbar);

        scrollOptions.axis = 'x';
        scrollOptions.mouseWheelNeedsShift = true;
        scrollbar = scout.create('Scrollbar', scrollOptions);
        scrollbars.push(scrollbar);
      } else {
        scrollbar = scout.create('Scrollbar', options);
        scrollbars.push(scrollbar);
      }
      $container.css('overflow', 'hidden');
      $container.data('scrollbars', scrollbars);
      scrollbars.forEach(function(scrollbar) {
        scrollbar.render($container);
        scrollbar.update();
      });
    }
  },

  /**
   * Removes the js scrollbars for the $container, if there are any.<p>
   * Also removes the scrollable from the detachhelper.
   */
  uninstall: function($container, session) {
    if (!$container.data('scrollable')) {
      // was not installed previously -> uninstalling not necessary
      return;
    }

    var scrollbars = $container.data('scrollbars');
    if (scrollbars) {
      scrollbars.forEach(function(scrollbar) {
        scrollbar.destroy();
      });
    }
    this.removeScrollable(session, $container);
    $container.removeData('scrollable');
    $container.css('overflow', '');
    $container.removeData('scrollbars');

    var htmlContainer = scout.HtmlComponent.optGet($container);
    if (htmlContainer) {
      htmlContainer.scrollable = false;
    }
  },

  /**
   * Recalculates the scrollbar size and position.
   * @param $scrollable JQuery element that has .data('scrollbars'), when $scrollable is falsy the function returns immediately
   * @param immediate set to true to immediately update the scrollbar, If set to false,
   *        it will be queued in order to prevent unnecessary updates.
   */
  update: function($scrollable, immediate) {
    if (!$scrollable || !$scrollable.data('scrollable')) {
      return;
    }
    var scrollbars = $scrollable.data('scrollbars');
    if (!scrollbars) {
      if (scout.device.isIos()) {
        this._handleIosPaintBug($scrollable);
      }
      return;
    }
    if (immediate) {
      this._update(scrollbars);
      return;
    }
    if ($scrollable.data('scrollbarUpdatePending')) {
      return;
    }
    // Executes the update later to prevent unnecessary updates
    setTimeout(function() {
      this._update(scrollbars);
      $scrollable.removeData('scrollbarUpdatePending');
    }.bind(this), 0);
    $scrollable.data('scrollbarUpdatePending', true);
  },

  _update: function(scrollbars) {
    // Reset the scrollbars first to make sure they don't extend the scrollSize
    scrollbars.forEach(function(scrollbar) {
      if (scrollbar.rendered) {
        scrollbar.reset();
      }
    });
    scrollbars.forEach(function(scrollbar) {
      if (scrollbar.rendered) {
        scrollbar.update();
      }
    });
  },

  /**
   * IOS has problems with nested scrollable containers. Sometimes the outer container goes completely white hiding the elements behind.
   * This happens with the following case: Main box is scrollable but there are no scrollbars because content is smaller than container.
   * In the main box there is a tab box with a scrollable table. This table has scrollbars.
   * If the width of the tab box is adjusted (which may happen if the tab item is selected and eventually prefSize called), the main box will go white.
   * <p>
   * This happens only if -webkit-overflow-scrolling is set to touch.
   * To workaround this bug the flag -webkit-overflow-scrolling will be removed if the scrollable component won't display any scrollbars
   */
  _handleIosPaintBug: function($scrollable) {
    if ($scrollable.data('scrollbarUpdatePending')) {
      return;
    }
    setTimeout(function() {
      workaround();
      $scrollable.removeData('scrollbarUpdatePending');
    });
    $scrollable.data('scrollbarUpdatePending', true);

    function workaround() {
      var size = scout.graphics.getSize($scrollable).subtract(scout.graphics.getInsets($scrollable, {
        includePadding: false,
        includeBorder: true
      }));
      if ($scrollable[0].scrollHeight === size.height && $scrollable[0].scrollWidth === size.width) {
        $scrollable.css('-webkit-overflow-scrolling', '');
      } else {
        $scrollable.css('-webkit-overflow-scrolling', 'touch');
      }
    }
  },

  reset: function($scrollable) {
    var scrollbars = $scrollable.data('scrollbars');
    if (!scrollbars) {
      return;
    }
    scrollbars.forEach(function(scrollbar) {
      scrollbar.reset();
    });
  },

  /**
   * Scrolls the $scrollable to the given $element (must be a child of $scrollable)
   */
  scrollTo: function($scrollable, $element) {
    var scrollTo,
      scrollOffsetUp = 4,
      scrollOffsetDown = 8,
      scrollableH = $scrollable.height(),
      elementBounds = scout.graphics.offsetBounds($element, false, false),
      scrollableBounds = scout.graphics.offsetBounds($scrollable, false, false),
      elementTop = elementBounds.y - scrollableBounds.y - scrollOffsetUp,
      elementH = elementBounds.height + scrollOffsetDown;

    //There are some elements which has a height of 0 (Checkboxes / Radiobuttons) -> try to get field and figure out its height and offset
    // TODO CGU remove this hack, fix checkbox and radio buttons
    if (elementH === scrollOffsetDown && $element.data('valuefield') && $element.data('valuefield').$container) {
      $element = $element.data('valuefield').$container;
      elementBounds = scout.graphics.offsetBounds($element, false, false);
      elementTop = elementBounds.y - scrollableBounds.y - scrollOffsetUp;
      elementH = elementBounds.height + scrollOffsetDown;
    }

    if (elementTop < 0) {
      scout.scrollbars.scrollTop($scrollable, $scrollable.scrollTop() + elementTop);
    } else if (elementTop + elementH > scrollableH) {
      // On IE, a fractional position gets truncated when using scrollTop -> ceil to make sure the full element is visible
      scrollTo = Math.ceil($scrollable.scrollTop() + elementTop + elementH - scrollableH);
      scout.scrollbars.scrollTop($scrollable, scrollTo);
    }
  },

  /**
   * Horizontally scrolls the $scrollable to the given $element (must be a child of $scrollable)
   *
   */
  scrollHorizontalTo: function($scrollable, $element) {
    var scrollTo,
      scrollableW = $scrollable.width(),
      elementBounds = scout.graphics.bounds($element, true, true),
      elementLeft = elementBounds.x,
      elementW = elementBounds.width;

    if (elementLeft < 0) {
      scout.scrollbars.scrollLeft($scrollable, $scrollable.scrollLeft() + elementLeft);
    } else if (elementLeft + elementW > scrollableW) {
      // On IE, a fractional position gets truncated when using scrollTop -> ceil to make sure the full element is visible
      scrollTo = Math.ceil($scrollable.scrollLeft() + elementLeft + elementW - scrollableW);
      scout.scrollbars.scrollLeft($scrollable, scrollTo);
    }
  },

  scrollTop: function($scrollable, scrollTop) {
    var scrollbar = scout.scrollbars.scrollbar($scrollable, 'y');
    if (scrollbar) {
      // js scrolling
      scrollbar.notifyBeforeScroll();
      $scrollable.scrollTop(scrollTop);
      scrollbar.notifyAfterScroll();
    } else {
      // native scrolling
      $scrollable.scrollTop(scrollTop);
    }
  },

  scrollLeft: function($scrollable, scrollLeft) {
    var scrollbar = scout.scrollbars.scrollbar($scrollable, 'x');
    if (scrollbar) {
      // js scrolling
      scrollbar.notifyBeforeScroll();
      $scrollable.scrollLeft(scrollLeft);
      scrollbar.notifyAfterScroll();
    } else {
      // native scrolling
      $scrollable.scrollLeft(scrollLeft);
    }
  },

  scrollbar: function($scrollable, axis) {
    var scrollbars = $scrollable.data('scrollbars') || [];
    return scout.arrays.find(scrollbars, function(scrollbar) {
      return scrollbar.axis === axis;
    });
  },

  scrollToBottom: function($scrollable) {
    scout.scrollbars.scrollTop($scrollable, $scrollable[0].scrollHeight - $scrollable[0].offsetHeight);
  },

  /**
   * Returns true if the location is visible in the current viewport of the $scrollable, or if $scrollable is null
   * @param location object with x and y properties
   *
   */
  isLocationInView: function(location, $scrollable) {
    if (!$scrollable || $scrollable.length === 0) {
      return true;
    }
    var scrollableOffsetBounds = scout.graphics.offsetBounds($scrollable);
    return scrollableOffsetBounds.contains(location.x, location.y);
  },

  /**
   * Attaches the given handler to each scrollable parent, including $anchor if it is scrollable as well.<p>
   * Make sure you remove the handlers when not needed anymore using offScroll.
   */
  onScroll: function($anchor, handler) {
    handler.$scrollParents = [];
    $anchor.scrollParents().each(function() {
      var $scrollParent = $(this);
      $scrollParent.on('scroll', handler);
      handler.$scrollParents.push($scrollParent);
    });
  },

  offScroll: function(handler) {
    var $scrollParents = handler.$scrollParents;
    if (!$scrollParents) {
      throw new Error('$scrollParents are not defined');
    }
    for (var i = 0; i < $scrollParents.length; i++) {
      var $elem = $scrollParents[i];
      $elem.off('scroll', handler);
    }
  },

  /**
   * Sets the position to fixed and updates left and top position.
   * This is necessary to prevent flickering in IE.
   */
  fix: function($elem) {
    if (!$elem.isVisible()) {
      return;
    }

    var bounds = scout.graphics.offsetBounds($elem);
    $elem
      .css('position', 'fixed')
      .cssLeft(bounds.x - $elem.cssMarginLeft())
      .cssTop(bounds.y - $elem.cssMarginTop())
      .cssWidth(bounds.width)
      .cssHeight(bounds.height);
  },

  /**
   * Reverts the changes made by fix().
   */
  unfix: function($elem, timeoutId) {
    clearTimeout(timeoutId);
    return setTimeout(function() {
      $elem.css({
        position: 'absolute',
        left: '',
        top: '',
        width: '',
        height: ''
      });
    }.bind(this), 50);
  },

  /**
   * Stores the position of all scrollables that belong to an optional session.
   * @param session (optional) when no session is given, scrollables from all sessions are stored
   */
  storeScrollPositions: function($container, session) {
    var $scrollables = this.getScrollables(session);
    if (!$scrollables) {
      return;
    }

    var scrollTop, scrollLeft;
    $scrollables.forEach(function($scrollable) {
      if ($container.isOrHas($scrollable[0])) {
        scrollTop = $scrollable.scrollTop();
        $scrollable.data('scrollTop', scrollTop);
        scrollLeft = $scrollable.scrollLeft();
        $scrollable.data('scrollLeft', $scrollable.scrollLeft());
        $.log.trace('Stored scroll position for ' + $scrollable.attr('class') + '. Top: ' + scrollTop + '. Left: ' + scrollLeft);
      }
    });
  },

  /**
   * Restores the position of all scrollables that belong to an optional session.
   * @param session (optional) when no session is given, scrollables from all sessions are restored
   */
  restoreScrollPositions: function($container, session) {
    var $scrollables = this.getScrollables(this.session);
    if (!$scrollables) {
      return;
    }

    var scrollTop, scrollLeft;
    $scrollables.forEach(function($scrollable) {
      if ($container.isOrHas($scrollable[0])) {
        scrollTop = $scrollable.data('scrollTop');
        if (scrollTop) {
          $scrollable.scrollTop(scrollTop);
          $scrollable.removeData('scrollTop');
        }
        scrollLeft = $scrollable.data('scrollLeft');
        if (scrollLeft) {
          $scrollable.scrollLeft(scrollLeft);
          $scrollable.removeData('scrollLeft');
        }
        // Also make sure that scroll bar is up to date
        // Introduced for use case: Open large table page, edit entry, press f5
        // -> outline tab gets rendered, scrollbar gets updated with set timeout, outline tab gets detached
        // -> update event never had any effect because it executed after detaching (due to set timeout)
        scout.scrollbars.update($scrollable);
        $.log.trace('Restored scroll position for ' + $scrollable.attr('class') + '. Top: ' + scrollTop + '. Left: ' + scrollLeft);
      }
    });
  }

};
