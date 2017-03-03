var GZIPWorker = undefined;

Ext.Ajax.requestGZ = function(config) {
	
	this.sendZipdata = function(body, conf) {
		conf.binaryData = body;
		conf.binary = true;
		
		var successfn = conf.success;
		if(successfn) {
			conf.success = function(response) {
				response.responseText = new TextDecoder('UTF-8').decode(response.responseBytes);
				successfn.call(this, response);
			}
		}
		var failurefn = conf.failure;
		if(failurefn) {
			conf.failure = function(response) {
				if(response.responseBytes)
					response.responseText = new TextDecoder('UTF-8').decode(response.responseBytes);
				failurefn.call(this, response);
			}
		}
		Ext.Ajax.request(conf);
	};
	
	config.method = 'post';
	if(!config.headers)
		config.headers = {};
	config.headers['Content-Encoding'] = 'gzip';
	
	var body = "";
	if(config.params) {
		for(var key in config.params) {
			if(body) body += "&";
			body += key + "=" + encodeURIComponent(config.params[key]);
		}
		delete config.params;
	}
	else if(config.rawData) {
		body = config.rawData;
		delete config.rawData;
		config.headers['Content-Type'] = 'application/json';
	}
	else if(config.jsonData) {
		body = config.jsonData;
		delete config.jsonData;
		config.headers['Content-Type'] = 'application/json';
	}

	if(body) {
		var me = this;
		if(typeof body == 'object')
			body = Ext.encode(body);
		
		if(window.Worker) {
		    if(GZIPWorker == undefined) {
		    	GZIPWorker = new Worker(CONTEXT_ROOT + "/js/workers/gzip.js");
		    }
		    GZIPWorker.postMessage(body);
		    GZIPWorker.onmessage = function(e) {
		    	me.sendZipdata(e.data, config);
		    }
		} else {
			Ext.Loader.loadScript({
				url: CONTEXT_ROOT + "/js/workers/jsonc.min.js",
				onLoad : function() {
					var zipbody = gzip.zip(body);
					me.sendZipdata(zipbody, config);
				}
			});
		}
	}
	else {
		Ext.Ajax.request(config);
	}
}