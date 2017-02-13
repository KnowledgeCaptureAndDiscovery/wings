Ext.Ajax.requestGZ = function(config) {
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
		if(typeof body == 'object')
			body = Ext.encode(body);
		config.binaryData = gzip.zip(body);
		config.binary = true;
		
		var successfn = config.success;
		if(successfn) {
			config.success = function(response) {
				response.responseText = new TextDecoder('UTF-8').decode(response.responseBytes);
				successfn.call(this, response);
			}
		}
		var failurefn = config.failure;
		if(failurefn) {
			config.failure = function(response) {
				if(response.responseBytes)
					response.responseText = new TextDecoder('UTF-8').decode(response.responseBytes);
				failurefn.call(this, response);
			}
		}		
	}

	Ext.Ajax.request(config);
};