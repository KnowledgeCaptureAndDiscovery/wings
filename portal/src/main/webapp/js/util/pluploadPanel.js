// Ext.ux.panel.UploadPanel for ExtJs 4 + plupload
// Source: http://www.thomatechnik.de/webmaster-tools/extjs-plupload/
// Based on: http://www.sencha.com/forum/showthread.php?98033-Ext.ux.Plupload-Panel-Button-%28file-uploader%29
// Please link to this page if you find this extension useful
// Version 0.2
// @authors:
// - mhd sulhan (ms@kilabit.org)

Ext.define(
				'Ext.ux.panel.UploadPanel',
				{
					extend : 'Ext.grid.Panel',
					alias : 'widget.xuploadpanel',

					// Configuration
					title : 'Upload',
					url : '',        // URL to your server-side upload-script
					pluploadPath : '/wings/lib/plupload/',     // Path to plupload
					chunk_size : '512kb',           // The chunk-size
					max_file_size : '1000mb',        // The max. allowed file-size
					unique_names : false,           // Make sure to use only unique-names
					multipart : true,               // Use multipart-uploads
					pluploadRuntimes : 'html5,gears,browserplus,silverlight,flash,html4', 
					multi_selection : false,
					
					// Texts (language-dependent)
					texts : {
						status : [ 'Queued', 'Uploading', 'Unknown', 'Failed',
								'Done' ],
						DragDropAvailable : 'Drag&drop files here',
						noDragDropAvailable : 'This Browser doesn\'t support drag&drop.',
						emptyTextTpl : '<div style="color:#808080; margin:0 auto; text-align:center; top:48%; position:relative;">{0}</div>',
						cols : [ "File", "Size", "State", "Mesage" ],
						addButtonText : 'Add file',
						uploadButtonText : 'Upload',
						cancelButtonText : 'Cancel',
						deleteButtonText : 'Delete',
						deleteUploadedText : 'Delete finished',
						deleteAllText : 'Delete all',
						deleteSelectedText : 'Delete selected',
						progressCurrentFile : 'Current file:',
						progressTotal : 'Total:',
						statusInvalidSizeText : 'File size is to big',
						statusInvalidExtensionText : 'Invalid file-type'
					},

					// Internal (do not change)
					// Grid-View
					multiSelect : true,
					viewConfig : {
						deferEmptyText : false     // For showing emptyText
					},

					// Hack: loaded of the actual file (plupload is sometimes a
					// step ahead)
					loadedFile : 0,

					constructor : function(config) {
						// List of files
						this.success = [];
						this.failed = [];

						// Column-Headers
						config.columns = [ {
							header : this.texts.cols[0],
							flex : 1,
							dataIndex : 'name'
						}, {
							header : this.texts.cols[1],
							flex : 1,
							align : 'right',
							dataIndex : 'size',
							renderer : Ext.util.Format.fileSize
						}, {
							header : this.texts.cols[2],
							flex : 1,
							dataIndex : 'status',
							renderer : this.renderStatus
						}, {
							header : this.texts.cols[3],
							flex : 1,
							dataIndex : 'msg'
						} ];

						// Model and Store
						if (!Ext.ModelManager.getModel('Plupload')) {
							Ext.define('Plupload', {
								extend : 'Ext.data.Model',
								fields : [ 'id', 'loaded', 'name', 'size',
										'percent', 'status', 'msg' ]
							});
						}

						config.store = {
							type : 'json',
							model : 'Plupload',
							listeners : {
								load : this.onStoreLoad,
								remove : this.onStoreRemove,
								update : this.onStoreUpdate,
								scope : this
							},
							proxy : 'memory'
						};

						// Top-Bar
						this.tbar = {
							enableOverflow : true,
							items : [
									new Ext.Button({
										text : this.texts.addButtonText,
										itemId : 'addButton',
										iconCls : config.addButtonCls
												|| 'pluploadAddCls',
										disabled : true
									}),
									new Ext.Button({
										text : this.texts.uploadButtonText,
										handler : this.onStart,
										scope : this,
										disabled : true,
										itemId : 'upload',
										iconCls : config.uploadButtonCls
												|| 'pluploadUploadCls'
									}),
									new Ext.Button({
										text : this.texts.cancelButtonText,
										handler : this.onCancel,
										scope : this,
										disabled : true,
										itemId : 'cancel',
										iconCls : config.cancelButtonCls
												|| 'pluploadCancelCls'
									}),
									new Ext.SplitButton(
											{
												text : this.texts.deleteButtonText,
												handler : this.onDeleteSelected,
												menu : new Ext.menu.Menu(
														{
															items : [
																	{
																		text : this.texts.deleteUploadedText,
																		handler : this.onDeleteUploaded,
																		scope : this
																	},
																	'-',
																	{
																		text : this.texts.deleteAllText,
																		handler : this.onDeleteAll,
																		scope : this
																	},
																	'-',
																	{
																		text : this.texts.deleteSelectedText,
																		handler : this.onDeleteSelected,
																		scope : this
																	} ]
														}),
												scope : this,
												disabled : true,
												itemId : 'delete',
												iconCls : config.deleteButtonCls
														|| 'pluploadDeleteCls'
											}) ]
						};

						// Progress-Bar (bottom)
						this.progressBarSingle = new Ext.ProgressBar({
							flex : 1,
							animate : true
						});
						this.progressBarAll = new Ext.ProgressBar({
							flex : 2,
							animate : true
						});

						this.bbar = {
							layout : 'hbox',
							style : {
								paddingLeft : '5px'
							},
							items : [ this.texts.progressCurrentFile,
									this.progressBarSingle, {
										xtype : 'tbtext',
										itemId : 'single',
										style : 'text-align:right',
										text : '',
										width : 100
									}, this.texts.progressTotal,
									this.progressBarAll, {
										xtype : 'tbtext',
										itemId : 'all',
										style : 'text-align:right',
										text : '',
										width : 100
									}, {
										xtype : 'tbtext',
										itemId : 'speed',
										style : 'text-align:right',
										text : '',
										width : 100
									}, {
										xtype : 'tbtext',
										itemId : 'remaining',
										style : 'text-align:right',
										text : '',
										width : 100
									} ]
						};

						this.callParent(arguments);
					}

					,
					afterRender : function() {
						this.callParent(arguments);
						this.initPlUpload();
					}

					,
					renderStatus : function(value, meta, record, rowIndex,
							colIndex, store, view) {
						var s = this.texts.status[value - 1];
						if (value == 2) {
							s += " " + record.get("percent") + " %";
						}
						return s;
					}

					,
					getTopToolbar : function() {
						var bars = this.getDockedItems('toolbar[dock="top"]');
						return bars[0];
					},
					getBottomToolbar : function() {
						var bars = this
								.getDockedItems('toolbar[dock="bottom"]');
						return bars[0];
					}

					,
					initPlUpload : function() {
						this.uploader = new plupload.Uploader({
							url : this.url,
							runtimes : this.pluploadRuntimes,
							browse_button : this.getTopToolbar().getComponent(
									'addButton').getEl().dom.id,
							container : this.getEl().dom.id,
							max_file_size : this.max_file_size || '',
							multi_selection : this.multi_selection,
							resize : this.resize || '',
							flash_swf_url : this.pluploadPath
									+ '/plupload.flash.swf',
							silverlight_xap_url : this.pluploadPath
									+ '/plupload.silverlight.xap',
							filters : this.filters || [],
							chunk_size : this.chunk_size,
							unique_names : this.unique_names,
							multipart : this.multipart,
							multipart_params : this.multipart_params || null,
							drop_element : this.getEl().dom.id,
							required_features : this.required_features || null
						});

						// Events
						Ext.each([ 'Init', 'ChunkUploaded', 'FilesAdded',
								'FilesRemoved', 'FileUploaded', 'PostInit',
								'QueueChanged', 'Refresh', 'StateChanged',
								'UploadFile', 'UploadProgress', 'Error' ],
								function(v) {
									this.uploader.bind(v, eval("this.Plupload"
											+ v), this);
								}, this);

						// Init Plupload
						this.uploader.init();
					}

					,
					onDeleteSelected : function() {
						Ext.each(this.getView().getSelectionModel()
								.getSelection(), function(record) {
							this.remove_file(record.get('id'));
						}, this);
					}

					,
					onDeleteAll : function() {
						this.store.each(function(record) {
							this.remove_file(record.get('id'));
						}, this);
					}

					,
					onDeleteUploaded : function() {
						this.store.each(function(record) {
							if (record.get('status') == 5) {
								this.remove_file(record.get('id'));
							}
						}, this);
					}

					,
					onCancel : function() {
						this.uploader.stop();
						this.updateProgress();
					}

					,
					onStart : function() {
						this.fireEvent('beforestart', this);

						if (this.multipart_params) {
							this.uploader.settings.multipart_params = this.multipart_params;
						}
						this.uploader.start();
					}

					,
					remove_file : function(id) {
						var fileObj = this.uploader.getFile(id);
						if (fileObj) {
							this.uploader.removeFile(fileObj);
						} else {
							this.store.remove(this.store.getById(id));
						}
					}

					,
					updateStore : function(files) {
						Ext.each(files, function(data) {
							this.updateStoreFile(data);
						}, this);
					}

					,
					updateStoreFile : function(data) {
						data.msg = data.msg || '';
						var record = this.store.getById(data.id);
						if (record) {
							record.set(data);
							record.commit();
						} else {
							this.store.add(data);
						}
					}

					,
					onStoreLoad : function(store, record, operation) {
					}

					,
					onStoreRemove : function(store, record, operation) {
						if (!store.data.length) {
							this.getTopToolbar().getComponent('delete')
									.setDisabled(true);
							this.uploader.total.reset();
						}
						var id = record.get('id');

						Ext.each(this.success, function(v) {
							if (v && v.id == id) {
								Ext.Array.remove(this.success, v);
							}
						}, this);

						Ext.each(this.failed, function(v) {
							if (v && v.id == id) {
								Ext.Array.remove(this.failed, v);
							}
						}, this);
					}

					,
					onStoreUpdate : function(store, record, operation) {
						var canUpload = false;
						if (this.uploader.state != 2) {
							this.store.each(function(record) {
								if (record.get("status") == 1) {
									canUpload = true;
									return false;
								}
							}, this);
						}
						this.getTopToolbar().getComponent('upload')
								.setDisabled(!canUpload);
					}

					,
					updateProgress : function(file) {
						var queueProgress = this.uploader.total;
						// All
						var total = queueProgress.size;
						var uploaded = queueProgress.loaded;
						this.getBottomToolbar().getComponent('all').setText(
								Ext.util.Format.fileSize(uploaded) + "/"
										+ Ext.util.Format.fileSize(total));

						if (total > 0) {
							this.progressBarAll.updateProgress(
									queueProgress.percent / 100,
									queueProgress.percent + " %");
						} else {
							this.progressBarAll.updateProgress(0, ' ');
						}

						// Speed+Remaining
						var speed = queueProgress.bytesPerSec;
						if (speed > 0) {
							var totalSec = parseInt((total - uploaded) / speed);
							var hours = parseInt(totalSec / 3600) % 24;
							var minutes = parseInt(totalSec / 60) % 60;
							var seconds = totalSec % 60;
							var timeRemaining = result = (hours < 10 ? "0"
									+ hours : hours)
									+ ":"
									+ (minutes < 10 ? "0" + minutes : minutes)
									+ ":"
									+ (seconds < 10 ? "0" + seconds : seconds);
							this.getBottomToolbar().getComponent('speed')
									.setText(
											Ext.util.Format.fileSize(speed)
													+ '/s');
							this.getBottomToolbar().getComponent('remaining')
									.setText(timeRemaining);
						} else {
							this.getBottomToolbar().getComponent('speed')
									.setText('');
							this.getBottomToolbar().getComponent('remaining')
									.setText('');
						}

						// Single
						if (!file) {
							this.getBottomToolbar().getComponent('single')
									.setText('');
							this.progressBarSingle.updateProgress(0, ' ');
						} else {
							total = file.size;
							uploaded = this.loadedFile;
							this.getBottomToolbar().getComponent('single')
									.setText(
											Ext.util.Format.fileSize(uploaded)
													+ "/"
													+ Ext.util.Format
															.fileSize(total));
							this.progressBarSingle.updateProgress(
									file.percent / 100, (file.percent)
											.toFixed(0)
											+ " %");
						}
					}

					,
					PluploadInit : function(uploader, data) {
						this.getTopToolbar().getComponent('addButton')
								.setDisabled(false);
						this.updateProgress();
					},

					PluploadChunkUploaded : function() {
					},

					PluploadFilesAdded : function(uploader, files) {
						this.getTopToolbar().getComponent('delete')
								.setDisabled(false);
						this.updateStore(files);
						this.updateProgress();
					},

					PluploadFilesRemoved : function(uploader, files) {
						Ext.each(files, function(file) {
							this.store.remove(this.store.getById(file.id));
						}, this);

						this.updateProgress();
					},
					
					PluploadFileUploaded : function(uploader, file, status) {
						var response = Ext.JSON.decode(status.response);
						if (response.success == true) {
							file.server_error = 0;
							file.location = response.location;
							this.success.push(file);
						} else {
							if (response.message) {
								file.msg = '<span style="color: red">'
										+ response.message + '</span>';
							}
							file.server_error = 1;
							this.failed.push(file);
						}
						this.updateStoreFile(file);
						this.updateProgress(file);
					},
					
					PluploadPostInit : function() {
					},
					
					PluploadQueueChanged : function(uploader) {
						this.updateProgress();
					},
					
					PluploadRefresh : function(uploader) {
						this.updateStore(uploader.files);
						this.updateProgress();
					},
					
					PluploadStateChanged : function(uploader) {
						if (uploader.state == 2) {
							this.fireEvent('uploadstarted', this);
							this.getTopToolbar().getComponent('cancel')
									.setDisabled(false);
						} else {
							this.fireEvent('uploadcomplete', this,
									this.success, this.failed);
							this.getTopToolbar().getComponent('cancel')
									.setDisabled(true);
						}
					},
					
					PluploadUploadFile : function() {
						this.loadedFile = 0;
					},
					
					PluploadUploadProgress : function(uploader, file) {
						// No chance to stop here - we get no response-text from
						// the server. So just continue if something fails here.
						// Will be fixed in next update, says plupload.
						if (file.server_error) {
							file.status = 4;
						}
						this.updateStoreFile(file);
						this.updateProgress(file);
						this.loadedFile = file.loaded;
					},
					
					PluploadError : function(uploader, data) {
						data.file.status = 4;
						if (data.code == -600) {
							data.file.msg = String.format(
									'<span style="color: red">{0}</span>',
									this.texts.statusInvalidSizeText);
						} else if (data.code == -700) {
							data.file.msg = String.format(
									'<span style="color: red">{0}</span>',
									this.texts.statusInvalidExtensionText);
						} else {
							data.file.msg = String
									.format(
											'<span style="color: red">{2} ({0}: {1})</span>',
											data.code, data.details,
											data.message);
						}
						this.updateStoreFile(data.file);
						this.updateProgress();
					}
				});


// Advance File-Size
Ext.util.Format.fileSize = function(value) {
	if (value > 1) {
		var s = [ 'Bytes', 'KB', 'MB', 'GB', 'TB', 'PB' ];
		var e = Math.floor(Math.log(value) / Math.log(1024));
		if (e > 0) {
			return (value / Math.pow(1024, Math.floor(e))).toFixed(2) + " "
					+ s[e];
		} else {
			return value + " " + s[e];
		}
	} else if (value == 1) {
		return "1 Byte";
	}
	return '-';
};

String.format = function() {
	var s = arguments[0];
	for ( var i = 0; i < arguments.length - 1; i++) {
		var reg = new RegExp("\\{" + i + "\\}", "gm");
		s = s.replace(reg, arguments[i + 1]);
	}
	return s;
};