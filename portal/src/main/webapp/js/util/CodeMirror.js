/**
* @author Adrian Teodorescu (ateodorescu@gmail.com; http://www.mzsolutions.eu)
* @docauthor Adrian Teodorescu (ateodorescu@gmail.com; http://www.mzsolutions.eu)
* @license [MIT][1]
*
* @version 1.5
*
*
* Provides a [CodeMirror][2] component wrapper for Sencha.
* The component was tested with Extjs 4.0.7, 4.1.x and 4.2.x and it works with CodeMirror 3.20.
*
* [1]: http://www.mzsolutions.eu/extjs/license.txt
* [2]: http://codemirror.net/
*
*
* The editor's toolbar buttons have tooltips defined in the {@link #buttonTips} property, but they are not
* enabled by default unless the global {@link Ext.tip.QuickTipManager} singleton is {@link Ext.tip.QuickTipManager#init initialized}.
*
* If you include the modes script files by yourself then ignore the {@link #modes} property.
* If you also include the extensions script files by yourself then ignore the {@link #extensions} property.
*
*
*
#Example usage:#

{@img Ext.ux.form.field.CodeMirror.png Ext.ux.form.field.CodeMirror component}

    var form = Ext.create('Ext.form.Panel', {
        title:          'Function info',
        bodyPadding:    10,
        width:          500,
        renderTo:       Ext.getBody(),
        items: [{
            xtype:      'textfield',
            name:       'name',
            anchor:     '100%',
            fieldLabel: 'Name',
            allowBlank: false  // requires a non-empty value
        }, {
            xtype:      'codemirror',
            name:       'function',
            fieldLabel: 'Code',
            anchor:     '100%'
        }],

        buttons: [{
            text: 'Save',
            handler: function(){
                if(form.getForm().isValid()){
                    alert(form.getForm().getValues().function);
                }
            }
        }]
    });


#Plugin example for the CodeMirror component:#

    Ext.define('Ext.ux.form.plugin.CodeMirror', {
        mixins: {
            observable: 'Ext.util.Observable'
        },
        alternateClassName: 'Ext.form.plugin.CodeMirror',
        requires: [
            'Ext.tip.QuickTipManager',
            'Ext.ux.form.field.CodeMirror'
        ],

        constructor: function(config) {
            Ext.apply(this, config);
        },

        init: function(codemirror){
            var me = this;
            me.codemirror = codemirror;
            me.mon(codemirror, 'initialize', me.onInitialize, me);
        },

        onInitialize: function(){
            var me = this, undef,
                items = [],
                baseCSSPrefix = Ext.baseCSSPrefix,
                tipsEnabled = Ext.tip.QuickTipManager && Ext.tip.QuickTipManager.isEnabled();

            function btn(id, toggle, handler){
                return {
                    itemId : id,
                    cls : baseCSSPrefix + 'btn-icon',
                    iconCls: baseCSSPrefix + 'edit-'+id,
                    enableToggle:toggle !== false,
                    scope: me,
                    handler:handler||me.relayBtnCmd,
                    clickEvent:'mousedown',
                    tooltip: tipsEnabled ? me.buttonTips[id] || undef : undef,
                    overflowText: me.buttonTips[id].title || undef,
                    tabIndex:-1
                };
            }

            items.push(btn('test', false));
            if(items.length > 0){
                me.codemirror.getToolbar().add(items);
            }
        },

        relayBtnCmd: function(btn){
            alert('test');
        },

        buttonTips : {
            test : {
                title: 'Test',
                text: 'Test button.',
                cls: Ext.baseCSSPrefix + 'html-editor-tip'
            }
        }

    });


*/

Ext.define('Ext.ux.layout.component.field.CodeMirror', {
	extend: 'Ext.layout.component.field.Field',
	alias: ['layout.codemirror'],

	toolbarSizePolicy: {
		setsWidth: 0,
		setsHeight: 0
	},

	beginLayout: function(ownerContext) {
		this.callParent(arguments);

		ownerContext.editorContext   = ownerContext.getEl('editorEl');
		ownerContext.toolbarContext  = ownerContext.getEl('toolbar');
	},

	renderItems: Ext.emptyFn,

	getItemSizePolicy: function (item) {
		return this.toolbarSizePolicy;
	},

	getLayoutItems: function () {
		var toolbar = this.owner.getToolbar();
		return toolbar ? [toolbar] : [];
	},

	getRenderTarget: function() {
		return this.owner.bodyEl;
	},

	publishInnerHeight: function (ownerContext, height) {
		var me = this,
		innerHeight = height - me.measureLabelErrorHeight(ownerContext) -
		ownerContext.bodyCellContext.getPaddingInfo().height;

		if(ownerContext.toolbarContext)
			innerHeight -= ownerContext.toolbarContext.getProp('height');

		if (Ext.isNumber(innerHeight)) {
			ownerContext.editorContext.setHeight(innerHeight);
		} else {
			me.done = false;
		}

		// hide the toolbar if there is no button visible
		if(this.owner.toolbar)
			if(this.owner.toolbar.items.length == 0){
				this.owner.toolbar.hide();
			}
	},

	/*publishInnerWidth: function (ownerContext, width) {
		var me = this;

		width = ownerContext.bodyCellContext.el.getWidth();
		if (Ext.isNumber(width)) {
			ownerContext.editorContext.setWidth(width);
			if(ownerContext.toolbarContext)
				ownerContext.toolbarContext.setWidth(width);
		} else {
			me.done = false;
		}
	}*/
});

Ext.define('Ext.ux.form.field.CodeMirror', {
    extend: 'Ext.form.field.Base',

    requires: [
        'Ext.tip.QuickTipManager',
        'Ext.toolbar.Item',
        'Ext.toolbar.Toolbar',
        'Ext.util.Format',
        'Ext.ux.layout.component.field.CodeMirror'
    ],

    alias: 'widget.codemirror',
    alternateClassName: 'Ext.form.CodeMirror',
    componentLayout: 'codemirror',

    childEls: [
        'toolbarEl', 'editorEl'
    ],

    fieldSubTpl: [
        '<div id="{cmpId}-toolbarEl"></div>',
        '<div id="{cmpId}-editorEl" class="{editorCls}" name="{editorName}" style="{size}"></div>',
        {
            disableFormats: true
        }
    ],

    editorWrapCls: Ext.baseCSSPrefix + 'html-editor-wrap ' + Ext.baseCSSPrefix + 'html-editor-input',

    maskOnDisable: true,

    /**
    * @cfg {String} mode The default mode to use when the editor is initialized. When not given, this will default to the first mode that was loaded.
    * It may be a string, which either simply names the mode or is a MIME type associated with the mode. Alternatively,
    * it may be an object containing configuration options for the mode, with a name property that names the mode
    * (for example {name: "javascript", json: true}). The demo pages for each mode contain information about what
    * configuration parameters the mode supports.
    */
    mode:               'text/plain',

    /**
    * @cfg {Boolean} showModes Enable mode selection in the toolbar
    */
    showModes:          false,
    
    showToolbar:		true,

    /**
    * @cfg {Boolean} enableMatchBrackets Force matching-bracket-highlighting to happen
    */
    enableMatchBrackets:    true,

    /**
    * @cfg {Boolean} enableElectricChars Configures whether the editor should re-indent the current line when a character is typed
    * that might change its proper indentation (only works if the mode supports indentation).
    */
    enableElectricChars:    false,

    /**
    * @cfg {Boolean} enableIndentWithTabs Whether, when indenting, the first N*tabSize spaces should be replaced by N tabs.
    */
    enableIndentWithTabs:   true,

    /**
    * @cfg {Boolean} enableSmartIndent Whether to use the context-sensitive indentation that the mode provides (or just indent the same as the line before).
    */
    enableSmartIndent:      true,

    /**
    * @cfg {Boolean} enableLineWrapping Whether CodeMirror should scroll or wrap for long lines.
    */
    enableLineWrapping:     false,

    /**
    * @cfg {Boolean} enableLineNumbers Whether to show line numbers to the left of the editor.
    */
    enableLineNumbers:      true,

    /**
    * @cfg {Boolean} enableGutter Can be used to force a 'gutter' (empty space on the left of the editor) to be shown even
    * when no line numbers are active. This is useful for setting markers.
    */
    enableGutter:           true,

    /**
    * @cfg {Boolean} enableFixedGutter When enabled (off by default), this will make the gutter stay visible when the
    * document is scrolled horizontally.
    */
    enableFixedGutter:      true,

    /**
    * @cfg {Number} firstLineNumber At which number to start counting lines.
    */
    firstLineNumber:         1,

    /**
     * @cfg {Boolean} readOnly <tt>true</tt> to mark the field as readOnly.
     */
    readOnly : false,

    /**
    * @cfg {Number} pollInterval Indicates how quickly (miliseconds) CodeMirror should poll its input textarea for changes.
    * Most input is captured by events, but some things, like IME input on some browsers, doesn't generate events
    * that allow CodeMirror to properly detect it. Thus, it polls.
    */
    pollInterval:         100,

    /**
    * @cfg {Number} indentUnit How many spaces a block (whatever that means in the edited language) should be indented.
    */
    indentUnit:         4,

    /**
    * @cfg {Number} tabSize The width of a tab character.
    */
    tabSize:            4,

    /**
    * @cfg {String} theme The theme to style the editor with. You must make sure the CSS file defining the corresponding
    * .cm-s-[name] styles is loaded (see the theme directory in the distribution). The default is "default", for which
    * colors are included in codemirror.css. It is possible to use multiple theming classes at once—for example
    * "foo bar" will assign both the cm-s-foo and the cm-s-bar classes to the editor.
    */
    theme:              'default',

    /**
    * @property {String} pathModes Path to the modes folder to dinamically load the required scripts. You could also
    * include all your required modes in a big script file and this path will be ignored.
    * Do not fill in the trailing slash.
    */
    pathModes:          'mode',

    /**
     * @cfg {Boolean} [allowBlank=true]
     * Specify false to validate that the value's length must be > 0.
     *
     */
    allowBlank : true,

    /**
     * @cfg {String} blankText
     * The error text to display if the **{@link #allowBlank}** validation fails
     */
    blankText : 'This field is required',

    /**
     * @cfg {Function} validator
     * A custom validation function to be called during field validation ({@link #getErrors}).
     * If specified, this function will be called first, allowing the developer to override the default validation
     * process.
     *
     * This function will be passed the following parameters:
     *
     * @cfg {Object} validator.value The current field value
     * @cfg {Boolean/String} validator.return
     *
     * - True if the value is valid
     * - An error message if the value is invalid
     */

    /**
    * @cfg {Array} listModes Define here what modes do you want to show in the selection list of the toolbar
    */
    listModes: [],

    /**
     * @cfg {Array} gutters Define here which gutter objects that you want included.
     */
    gutters: [ "CodeMirror-linenumbers" ],

    /**
     * @cfg {Boolean} foldGutter Define here if you want to display fold gutters.
     * If you do make sure you add "CodeMirror-foldgutter" to the gutters config
     * array.
     */
    foldGutter: false,

    /**
    * @property {Array} modes Define here mode script dependencies; When choosing a specific mode the script files are automatically loaded
    */
    modes: [
            {mime: 'text/plain'},
            {mime: 'text/x-csrc'},
            {mime: 'text/x-c++src'},
            {mime: 'text/x-csharp'},
            {mime: 'text/css'},
            {mime: 'django'},
            {mime: 'go'},
            {mime: 'text/html'},
            {mime: 'http'},
            {mime: 'text/x-java'},
            {mime: 'text/javascript'},
            {mime: 'application/x-jena'},
            {mime: 'text/x-objectivec'},
            {mime: 'text/x-perl'},
            {mime: 'text/x-php'},
            {mime: 'text/x-python'},
            {mime: 'text/x-rsrc'},
            {mime: 'text/x-sh'},
            {mime: 'swift'},
            {mime: 'sql'},
            {mime: 'text/xml'}
    ],

    scriptsLoaded: [],
    lastMode: '',

    initComponent : function(){
        var me = this;

        me.addEvents(
            /**
             * @event initialize
             * Fires when the editor is fully initialized (including the iframe)
             * @param {Ext.ux.form.field.CodeMirror} this
             */
            'initialize',
            /**
             * @event activate
             * Fires when the editor is first receives the focus. Any insertion must wait
             * until after this event.
             * @param {Ext.ux.form.field.CodeMirror} this
             */
            'activate',
            /**
             * @event deactivate
             * Fires when the editor looses the focus.
             * @param {Ext.ux.form.field.CodeMirror} this
             */
            'deactivate',
             /**
             * @event change
             * Fires when the content of the editor is changed.
             * @param {Ext.ux.form.field.CodeMirror} this
             * @param {String} newValue New value
             * @param {String} oldValue Old value
             * @param {Array} options
             */
            'change',
             /**
             * @event modechanged
             * Fires when the editor mode changes.
             * @param {Ext.ux.form.field.CodeMirror} this
             * @param {String} newMode New mode
             * @param {String} oldMode Old mode
             */
            'modechanged',
            /**
             * @event cursoractivity
             * Fires when the cursor or selection moves, or any change is made to the editor content.
             * @param {Ext.ux.form.field.CodeMirror} this
             */
            'cursoractivity',
            /**
             * @event gutterclick
             * Fires whenever the editor gutter (the line-number area) is clicked.
             * @param {Ext.ux.form.field.CodeMirror} this
             * @param {Number} lineNumber Zero-based number of the line that was clicked
             * @param {Object} event The raw mousedown event
             */
            'gutterclick',
            /**
             * @event scroll
             * Fires whenever the editor is scrolled.
             * @param {Ext.ux.form.field.CodeMirror} this
             */
            'scroll',
            /**
             * @event update
             * Fires whenever CodeMirror updates its DOM display.
             * @param {Ext.ux.form.field.CodeMirror} this
             */
            'update',
            /**
             * @event keypress
             * Fired when CodeMirror is handling a DOM event of this type. You can preventDefault the event,
             * or give it a truthy codemirrorIgnore property, to signal that CodeMirror should do no further handling.
             * @param {Ext.ux.form.field.CodeMirror} this
             * @param {Object} event The key event
             */
            'keypress',
            /**
             * @event keydown
             * Fired when CodeMirror is handling a DOM event of this type. You can preventDefault the event,
             * or give it a truthy codemirrorIgnore property, to signal that CodeMirror should do no further handling.
             * @param {Ext.ux.form.field.CodeMirror} this
             * @param {Object} event The key event
             */
            'keydown',
            /**
             * @event keyup
             * Fired when CodeMirror is handling a DOM event of this type. You can preventDefault the event,
             * or give it a truthy codemirrorIgnore property, to signal that CodeMirror should do no further handling.
             * @param {Ext.ux.form.field.CodeMirror} this
             * @param {Object} event The key event
             */
            'keyup'
        );


        me.callParent(arguments);

        me.initLabelable();
        me.initField();

        /*
        Fix resize issues as suggested by user koblass on the Extjs forums
        http://www.sencha.com/forum/showthread.php?167047-Ext.ux.form.field.CodeMirror-for-Ext-4.x&p=860535&viewfull=1#post860535
        */
        me.on('resize', me.onFieldResize, me);

    },

    getMaskTarget: function(){
        return this.bodyEl;
    },

    /**
    * @private override
    */
    getSubTplData: function() {
        var me = this,
            cssPrefix = Ext.baseCSSPrefix;
            
        return {
            $comp           : me,
            cmpId           : me.id,
            id              : me.getInputId(),
            toolbarWrapCls  : cssPrefix + 'html-editor-tb',
            textareaCls     : cssPrefix + 'hidden',
            editorCls       : cssPrefix + 'codemirror ' + me.editorWrapCls,
            editorName      : Ext.id(),
            size            : 'height:100px;width:100%'
        };
    },

    /**
    * @private override
    */
    afterRender: function() {
        var me = this;

        me.callParent(arguments);

        me.inputEl = me.editorEl;
        if(this.showToolbar)
        	me.createToolbar();
        me.initEditor();
    },

    onFieldResize: function() {
        var me = this;

        if (me.editor) {
            me.editor.refresh();
        }
    },

    /**
    * @private override
    */
    initEditor : function(){
        var me = this,
            mode = 'text/plain';

        // if no mode is loaded we could get an error like "Object #<Object> has no method 'startState'"
        // search mime to find script dependencies
        var item = me.getMime(me.mode);
        if(item) {
            mode = me.getMimeMode(me.mode);
            if(!mode){
                mode = 'text/plain';
            }
        }

        me.editor = CodeMirror(me.editorEl.dom, {
            matchBrackets:      me.enableMatchBrackets,
            electricChars:      me.enableElectricChars,
            autoClearEmptyLines:true,
            value:              me.rawValue || '',
            indentUnit:         me.indentUnit,
            smartIndent:        me.enableSmartIndent,
            indentWithTabs:     me.indentWithTabs,
            pollInterval:       me.pollInterval,
            lineNumbers:        me.enableLineNumbers,
            lineWrapping:       me.enableLineWrapping,
            firstLineNumber:    me.firstLineNumber,
            tabSize:            me.tabSize,
            gutter:             me.enableGutter,
            fixedGutter:        me.enableFixedGutter,
            theme:              me.theme,
            gutters:            me.gutters,
            foldGutter:         me.foldGutter,
            mode:               mode
        });

        // CodeMirror doesn't allow "scope" to be given to the event handler so we workaround it
        me.editor.parentField = me;
        me.editor.on('change', me.onEditorChange);
        me.editor.on('cursorActivity', me.onEditorCursorActivity);
        me.editor.on('gutterClick', me.onEditorGutterClick);
        me.editor.on('focus', me.onEditorFocus);
        me.editor.on('blur', me.onEditorBlur);
        me.editor.on('scroll', me.onEditorScroll);
        me.editor.on('update', me.onEditorUpdate);
        me.editor.on('keypress', me.onEditorKeypress);
        me.editor.on('keydown', me.onEditorKeydown);
        me.editor.on('keyup', me.onEditorKeyup);

        me.setMode(me.mode);
        me.setReadOnly(me.readOnly);
        me.fireEvent('initialize', me);

        // change the codemirror css
        var css = Ext.util.CSS.getRule('.CodeMirror');
        if(css){
            css.style.height = '100%';
            css.style.position = 'relative';
            css.style.overflow = 'hidden';
        }
        var css = Ext.util.CSS.getRule('.CodeMirror-Scroll');
        if(css){
            css.style.height = '100%';
        }

    },

    /**
    * @private
    */
    onEditorChange: function(editor, changeObj){
        var me = editor.parentField;

        me.checkChange();
    },

    /**
    * @private
    */
    onEditorCursorActivity: function(editor){
        var me = editor.parentField;

        me.fireEvent('cursoractivity', me);
    },

    /**
    * @private
    */
    onEditorGutterClick: function(editor, line, gutter, event){
        var me = editor.parentField;

        me.fireEvent('gutterclick', me, line, gutter, event);
    },

    /**
    * @private
    */
    onEditorFocus: function(editor){
        var me = editor.parentField;

        me.fireEvent('activate', me);
    },

    /**
    * @private
    */
    onEditorBlur: function(editor){
        var me = editor.parentField;

        me.fireEvent('deactivate', me);
    },

    /**
    * @private
    */
    onEditorScroll: function(editor){
        var me = editor.parentField;

        me.fireEvent('scroll', me);
    },

    /**
    * @private
    */
    onEditorUpdate: function(editor){
        var me = editor.parentField;

        me.fireEvent('update', me);
    },

    /**
    * @private
    */
    onEditorKeypress: function(editor, event){
        var me = editor.parentField;

        event.cancelBubble = true; // fix suggested by koblass user on Sencha forums (http://www.sencha.com/forum/showthread.php?167047-Ext.ux.form.field.CodeMirror-for-Ext-4.x&p=862029&viewfull=1#post862029)
        me.fireEvent('keypress', me, event);
    },

    /**
    * @private
    */
    onEditorKeydown: function(editor, event){
        var me = editor.parentField;

        event.cancelBubble = true; // fix suggested by koblass user on Sencha forums (http://www.sencha.com/forum/showthread.php?167047-Ext.ux.form.field.CodeMirror-for-Ext-4.x&p=862029&viewfull=1#post862029)
        me.fireEvent('keydown', me, event);
    },

    /**
    * @private
    */
    onEditorKeyup: function(editor, event){
        var me = editor.parentField;

        event.cancelBubble = true; // fix suggested by koblass user on Sencha forums (http://www.sencha.com/forum/showthread.php?167047-Ext.ux.form.field.CodeMirror-for-Ext-4.x&p=862029&viewfull=1#post862029)
        me.fireEvent('keyup', me, event);
    },

    /**
    * @private
    */
    createToolbar : function(editor){
        var me = this,
            items = [],
            tipsEnabled = Ext.tip.QuickTipManager && Ext.tip.QuickTipManager.isEnabled(),
            baseCSSPrefix = Ext.baseCSSPrefix,
            toolbar, undef;

        var me = this;

        me.toolbar = Ext.create('Ext.toolbar.Toolbar', {
            /*id:                 me.id + '-toolbar',*/
            cls:                Ext.baseCSSPrefix + 'html-editor-tb',
            renderTo:           me.toolbarEl,
            enableOverflow:     true,
            items:              items,
            style: "border-top:0px",

            listeners: {
                click: function(e){
                    e.preventDefault();
                },
                element: 'el'
            }
        });
        me.updateToolbarButtons();
    },

    getToolbar : function(){
        return this.toolbar;
    },

    updateToolbarButtons: function(){
        var me = this;
        try{
            btns = me.getToolbar().items.map;
        }catch(err){
        }
    },

    /**
    * @private
    */
    relayBtnCmd: function(btn){
        this.relayCmd(btn.getItemId());
    },

    /**
    * @private
    */
    relayCmd: function(cmd){
        Ext.defer(function() {
            var me = this;
            me.editor.focus();
            switch(cmd){
                // line numbers
                case 'insertorderedlist':
                    me.doChangeLineNumbers();
                break;
            }
        }, 10, this);
    },

    doChangeLineNumbers: function(){
        var me = this;

        me.enableLineNumbers = !me.enableLineNumbers;
        me.editor.setOption('lineNumbers', me.enableLineNumbers);
    },

    /**
    * @private
    */
    getMime: function(mime){
        var me = this, item, found = false;
        for(var i=0;i<me.modes.length;i++){
            item = me.modes[i];
            if(Ext.isArray(item.mime)){
                if(Ext.Array.contains(item.mime, mime)){
                    found = true;
                    break;
                }
            }else{
                if(item.mime == mime){
                    found = true;
                    break;
                }
            }
        }
        if(found)
            return item;
        else
            return null;
    },

    /**
    * @private
    */
    loadDependencies: function(item, path, handler, scope){
        var me = this;

        me.scripts = [];
        me.scriptIndex = -1;

        if(!item.dependencies)
        	return;
        
        // load the dependencies
        for(var i=0; i < item.dependencies.length; i++){
            if(!Ext.Array.contains(me.scriptsLoaded, path + '/' + item.dependencies[i])){
                var options = {
                    url: path + '/' + item.dependencies[i],
                    index: ++me.scriptIndex,
                    onLoad: function(options){
                        var ok = true;
                        for(j=0; j < me.scripts.length; j++){
                            if(me.scripts[j].called) {// this event could be raised before one script if fetched
                                ok = ok && me.scripts[j].success;
                                if(me.scripts[j].success && !Ext.Array.contains(me.scriptsLoaded, me.scripts[j].url)){
                                    me.scriptsLoaded.push(me.scripts[j].url);
                                }
                            }else{
                                ok = false;
                            }
                        }
                        if(ok){
                            handler.call(scope || me.editor);
                        }
                    }
                };

                me.scripts[me.scriptIndex] = {
                    url: options.url,
                    success: true,
                    called: false,
                    options: options,
                    onLoad: options.onLoad || Ext.emptyFn,
                    onError: options.onError || Ext.emptyFn
                };
            }
        }
        for(var i=0; i < me.scripts.length; i++){
            me.loadScript(me.scripts[i].options);
        }
    },

    /**
    * @private
    */
    loadScript: function(options){
        var me = this;
        Ext.Ajax.request({
            url: options.url,
            scriptIndex: options.index,
            success: function(response, options) {
                var script = 'Ext.getCmp("' + this.id + '").scripts[' + options.scriptIndex + ']';
                window.setTimeout('try { ' + response.responseText + ' } catch(e) { '+script+'.success = false; '+script+'.onError('+script+'.options, e); };  ' + script + '.called = true; if ('+script+'.success) '+script+'.onLoad('+script+'.options);', 0);
            },
            failure: function(response, options) {
                var script = this.scripts[options.scriptIndex];
                script.success = false;
                script.called = true;
                script.onError(script.options, response.status);
            },
            scope: me
        });
    },

    /**
    * @private
    * Return mode depending on the mime; If the mime is not loaded then return null
    *
    * @param mime
    */
    getMimeMode: function(mime){
        return CodeMirror.mimeModes[mime];
    },

    /**
    * Change the CodeMirror mode to the specified mime.
    *
    * @param {String} mime The MIME value according to the CodeMirror documentation
    */
    setMode: function(mime){
        var me = this,
            found = false;
        // search mime to find script dependencies
        var item = me.getMime(mime);
        if(!item) {
            // mime not found
            return;
        }

        var mode = me.getMimeMode(mime);

        if(!mode){
            me.loadDependencies(item, me.pathModes, function(){
                var mode = me.getMimeMode(mime);
                if(typeof mode == "string")
                    me.editor.setOption('mode', mime);
                else
                    me.editor.setOption('mode', mode);
            });
        }else{
            if(typeof mode == "string")
                me.editor.setOption('mode', mime);
            else
                me.editor.setOption('mode', mode);
        }

        if(me.modesSelect){
            me.modesSelect.dom.value = mime;
        }
        try{
            me.fireEvent('modechanged', me, mime, me.lastMode);
            me.lastMode = mime;
        }catch(err){}
    },

    /**
    * Set the editor as read only
    *
    * @param {Boolean} readOnly
    */
    setReadOnly: function(readOnly) {
        var me = this;

        if(me.editor){
            me.editor.setOption('readOnly', readOnly);
            me.disableItems(readOnly);
        }
    },

    onDisable: function() {
        this.bodyEl.mask();
        this.callParent(arguments);
    },

    onEnable: function() {
        this.bodyEl.unmask();
        this.callParent(arguments);
    },

    disableItems: function(disabled) {
    	if(this.showToolbar)
    		this.getToolbar().items.each(function(item){
    			item.setDisabled(disabled);
    		});
    },

    /**
    * Sets a data value into the field and runs the change detection. 
    * @param {Mixed} value The value to set
    * @return {Ext.ux.form.field.CodeMirror} this
    */
    setValue: function(value){
        var me = this;
        me.mixins.field.setValue.call(me, value);
        me.rawValue = value;
        if(me.editor)
            me.editor.setValue(value);
        return me;
    },

    /**
    * Return submit value to the owner form.
    * @return {Mixed} The field value
    */
    getSubmitValue: function(){
        return this.getValue();
    },
    
    /**
    * Return raw value to the owner form.
    * @return {Mixed} The field value
    */
    getRawValue: function(){
        return this.getValue();
    },

    /**
    * Return the value of the CodeMirror editor
    * @return {Mixed} The field value
    */
    getValue: function(){
        var me = this;
        
        return me.editor ? me.editor.getValue() : null;
    },
    
    /**
    * Validates the field value according to the field's validation rules and returns an array
    * of errors for any failing validations. Validation rules are processed in the following order:
    * 
    * 1. **Field specific validator**
    *
    *     A validator offers a way to customize and reuse a validation specification.
    *     If a field is configured with a `{@link #validator}`
    *     function, it will be passed the current field value.  The `{@link #validator}`
    *     function is expected to return either:
    *
    *     - Boolean `true`  if the value is valid (validation continues).
    *     - a String to represent the invalid message if invalid (validation halts).
    *
    * 2. **Basic Validation**
    *
    *     If the `{@link #validator}` has not halted validation,
    *     basic validation proceeds as follows:
    *
    *     - `{@link #allowBlank}` : (Invalid message = `{@link #blankText}`)
    *
    *         Depending on the configuration of `{@link #allowBlank}`, a
    *         blank field will cause validation to halt at this step and return
    *         Boolean true or false accordingly.
    * 
    * 
    */
    getErrors: function(value) {
        var me = this,
            errors = me.callParent(arguments),
            validator = me.validator,
            msg, trimmed;

        value = value || me.processRawValue(me.getRawValue()) || '';

        if (Ext.isFunction(validator)) {
            msg = validator.call(me, value);
            if (msg !== true) {
                errors.push(msg);
            }
        }
        
        trimmed = Ext.String.trim(value);

        if (trimmed.length < 1) {
            if (!me.allowBlank) {
                errors.push(me.blankText);
            }
        }

        return errors;
    },

    /**
    * @private
    */
    onDestroy: function(){
        var me = this,
            prop;

        me.un('resize', me.onFieldResize, me);

        if(me.rendered){
            try {
                delete(me.editor.parentField);
                me.editor.un('change', me.onEditorChange);
                me.editor.un('cursorActivity', me.onEditorCursorActivity);
                me.editor.un('gutterClick', me.onEditorGutterClick);
                me.editor.un('focus', me.onEditorFocus);
                me.editor.un('blur', me.onEditorBlur);
                me.editor.un('scroll', me.onEditorScroll);
                me.editor.un('update', me.onEditorUpdate);
                me.editor.un('keypress', me.onEditorKeypress);
                me.editor.un('keydown', me.onEditorKeydown);
                me.editor.un('keyup', me.onEditorKeyup);

                for (prop in me.editor) {
                    if (me.editor.hasOwnProperty(prop)) {
                        delete me.editor[prop];
                    }
                }
            }catch(e){}
            Ext.destroyMembers('toolbar', 'editorEl');
        }
        me.callParent();
    },

    /**
    * Object collection of toolbar tooltips for the buttons in the editor. The key
    * is the command id associated with that button and the value is a valid QuickTips object.
    * These are taken from the HtmlEditor to avoid including additional css.
    * @type Object
    */
    buttonTips : {
        insertorderedlist : {
            title: 'Line numbers',
            text: 'Show line numbers.',
            cls: Ext.baseCSSPrefix + 'html-editor-tip'
        }
    }

});
