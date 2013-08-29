var ControlList = function(data, bindings) {
	this._tasks = [];
	this._taskMap = {};
	if(bindings) {
		this.expandBindings(bindings);
	}
   this.addParaphrases(data, bindings);
};

ControlList.prototype.tasks = function(_tasks) {
	if(_tasks) this._tasks = _tasks;
	else return this._tasks;
};

ControlList.prototype.getTaskByName = function(name) {
	return this._taskMap[name];
	/*for (var i in this._tasks) {
		var task = this._tasks[i];
		if(typeof(task) == 'function') continue;
		if(task._text == name) return task;
	}
	return undefined;*/
};

ControlList.prototype.addTask = function(task) {
	if(task) {
		this._tasks.push(task);
		this._taskMap[task._text] = task;
	}
};

ControlList.prototype.expandBindings = function(bindings) {
	for(var arg in bindings) {
		var data = bindings[arg].data;
		if(bindings[arg].type=="reverse") {
			var data = bindings[arg].data;
			for(var term in data) {
				var paras = data[term];
				var nparas = [];
				for(var i=0; i<paras.length; i++) {
					var xp = this.expandParaphraseString([paras[i]]);
					nparas = nparas.concat(xp);
				}
				data[term] = nparas;
			}
		}
		else {
			var ndata = {};
			for(var para in data) {
				var map = data[para];
				var xp = this.expandParaphraseString([para]);
				for(var i=0; i<xp.length; i++) {
					ndata[xp[i]] = map;
				}
			}
			bindings[arg].data = ndata;
		}
	}
};

ControlList.prototype.addParaphrases = function(data, bindings) {
	for(var currentTaskFrame in data) {
		var pstrings = data[currentTaskFrame];
		if(typeof(pstrings) == 'function') continue;
		for(var i=0; i<pstrings.length; i++) {
			var xp =  this.expandParaphraseString( [pstrings[i]] );
			for(var j=0; j<xp.length; j++) {
				var paras = this.getBoundParaphrases(new Paraphrase(xp[j]), currentTaskFrame, bindings);
				for(var k=0; k<paras.length; k++) {
					var frame = paras[k][0];
					var para = paras[k][1];
					var task = this.getTaskByName(frame);
					if(!task) {
						task = new Task(frame);
						this.addTask(task);
					}
					task._paraphrases.push(para);
				}
			}
		}
	}
	//if(window.console) window.console.log(this);
};

ControlList.prototype.getBoundParaphrases = function(para, frame, data) {
	var paras = [];
	var add_orig = true;
	for(var i=0; i<para.tokens().length; i++) {
		var token = para.getToken(i);
		var tdata = data[token.name()];
		if(token.type() == "arg" && tdata) {
			var tok_name = tdata['mapping'];
			var tok_data = tdata['data'];
			var tok_type = tdata['type'];
			if(token.name() != tok_name) add_orig = false;
			if(tok_type == 'reverse') {
				for(var comp in tok_data) {
					var kwds = tok_data[comp];
					if(typeof(kwds) == 'function') continue;
					var tframe = frame.replace(" +"+tok_name, " +"+tok_name+"="+comp);
					for(var j=0; j<kwds.length; j++) {
						var kwd = kwds[j];
						var ttext = para._text.replace("+"+token.name()+" ", kwd+" ")
													.replace("+"+token.name(), kwd);
						if(tframe != frame || ttext != para._text) {
							var tpara = new Paraphrase(ttext);
							paras.push([tframe, tpara]);
						}
					}
				}
			}
			else {
				for(var kwd in tok_data) {
					var comps = tok_data[kwd];
					if(typeof(comps) == 'function') continue;
					var ttext = para._text.replace("+"+token.name()+" ", kwd+" ")
												.replace("+"+token.name(), kwd);
					var tpara = new Paraphrase(ttext);
					for(var j=0; j<comps.length; j++) {
						var tframe = frame.replace(" +"+tok_name, " +"+tok_name+"="+comps[j]);
												//.replace(" +"+tok_name, " +"+tok_name+"="+comps[j]);
						if(tframe != frame || ttext != para._text)
							paras.push([tframe, tpara]);
					}
				}
			}
		}
	}
	if(add_orig) paras.push([frame, para]);
	return paras;
};

// - Expand the paraphrase string (if in shortcut notation) into multiple paraphrase strings
// - Return an array of paraphrase strings
//   (Recursive function. first check for expansion)
ControlList.prototype.expandParaphraseString = function(paras) {
   var newparas = [];
   var newinfo = 0;
	for(var pi=0; pi<paras.length; pi++) {
		var para = paras[pi];
      var altstr = '';
      var optstr = '';
		var chars = para.split("");
      for(var i=0; i<chars.length; i++) {
			var c = chars[i];
         // Check for Alt shortcut
         if(c == "|") {
            var j = para.substr(i+1).indexOf('|');
            if(j>0) {
               altstr = para.substr(i+1,j);
               break;
            }
         }
         // Check for Opt shortcut
         if(c == "{") {
            var j = para.substr(i+1).indexOf('}');
            if(j>0) {
               optstr = para.substr(i+1,j);
               break;
            }
         }
      }

      if(altstr) {
         var alts = altstr.split('/');
         //var e_altstr = "\\\|"+altstr.escape_regex()+"\\\|";
         for(var ai in alts) {
				var alt = alts[ai];
				if(typeof(alt) == 'function') continue;
            //var newpara = para.replace(new RegExp(e_altstr), alt); 
            var newpara = para.replace("|"+altstr+"|", alt); 
            newparas.push(newpara.trim());
         }
         newinfo=1;
      }
      else if(optstr) {
         //var e_optreg = new RegExp("\\\{"+optstr.escape_regex()+"\\\} ");
			var _without = para.replace("{"+optstr+"}", '').replace(/\s+/,' ');
         var _with = para.replace("{"+optstr+"} ", optstr+' ').replace("{"+optstr+"}", optstr);
         newparas.push(_without.trim());
         newparas.push(_with.trim());
         newinfo=1;
      }
      else {
         newparas.push(para);
      }
   }
   if(newinfo) {
      return this.expandParaphraseString(newparas);
   }
   return paras;
};

String.prototype.trim = function () {
    return this.replace(/^\s*/, "").replace(/\s*$/, "");
};

String.prototype.escape_regex = function () {
	return this.replace(/\//g, '\\\/')
		.replace(/\|/g,'\\\|')
		.replace(/\./g,'\\\.')
		.replace(/\(/g,'\\\(')
		.replace(/\)/g,'\\\)')
		.replace(/\[/g,'\\\[')
		.replace(/\]/g,'\\\]')
		.replace(/\*/g,'\\\*')
		.replace(/\?/g,'\\\?')
		.replace(/\+/g,'\\\+');
};
