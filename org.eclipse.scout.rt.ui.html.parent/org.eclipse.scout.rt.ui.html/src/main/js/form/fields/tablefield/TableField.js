// SCOUT GUI
// (c) Copyright 2013-2014, BSI Business Systems Integration AG

scout.TableField = function() {
  scout.TableField.parent.call(this);
  this._addAdapterProperties(['table']);
};
scout.inherits(scout.TableField, scout.FormField);

scout.TableField.prototype._render = function($parent) {
  this.addContainer($parent, 'TableField');
  this.$container.addClass('table-field');
  this.addLabel();
  this.addStatus();

  if (this.table) {
    this._renderTable();
  }
};

/**
 * Will also be called by model adapter on property change event
 */
scout.TableField.prototype._renderTable = function() {
  this.table.render(this.$container);
  this.table.$container.addClass('field');
};
