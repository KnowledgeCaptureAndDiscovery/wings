var TodoListParser = function(paraphrases, mapping) {
	this._todo_entries     = [];
	this._paraphrases      = paraphrases;
	this._paraphrases.io   = this.createIOParaphrases(paraphrases);
	this._st_control_list  = new ControlList(paraphrases.statement, []);
	this._c_control_list   = new ControlList(paraphrases.io, mapping.component);
	this._d_control_list   = new ControlList(paraphrases.data, mapping.data);
	this._p_control_list   = new ControlList(paraphrases.property, mapping.property);

	this._st_trie          = this.getTrieForList(this._st_control_list, 0);
	this._c_trie           = this.getTrieForList(this._c_control_list, 1);
	this._d_trie           = this.getTrieForList(this._d_control_list, 2);
	this._p_trie           = this.getTrieForList(this._p_control_list, 3);

	this._matching_algo    = "ordered"; // ordered, regex
	this._top_matches_only = true;
	//this._stem_words       = false;
	//this._use_wordnet      = false;
	//this._wn_handle        = undefined;
};

TodoListParser.prototype.getCControlList = function() {
	return this._c_control_list;
};

TodoListParser.prototype.getDControlList = function() {
	return this._d_control_list;
};

TodoListParser.prototype.getStControlList = function() {
	return this._st_control_list;
};

TodoListParser.prototype.getPControlList = function() {
	return this._p_control_list;
};

TodoListParser.prototype.createIOParaphrases = function(paraphrases) {
	var ioParaphrases = {};
	var reg1 =  new RegExp("\\\+(\\\w+)");
	var reg2 = new RegExp("\\\+\\\w+\\\[(.+)=(.+)\\\]");
	for(var taskFrame in paraphrases.component) {
		var paras = paraphrases.component[taskFrame];
		while(paras.length > 0) {
			var para = paras.pop();
			pwords = para.split(/\s+/);
			var replaced = false;
			for(var i=0; i<pwords.length; i++) {
				var pword = pwords[i];
				var m = reg1.exec(pword);
				var m2 = reg2.exec(pword);
				if(m) {
					try {
						eval("var opts = paraphrases."+m[1]+";");
						if(opts && opts.length) {
							replaced = true;
							for(var j=0; j<opts.length; j++) {
								var opt = opts[j];
								var tmp;
								if(m2) {
									opt = opt.replace("+"+m2[1], "+"+m2[2]);
									tmp = para.replace("+"+m[1]+"["+m2[1]+"="+m2[2]+"]", opt);
								} else {
									tmp = para.replace("+"+m[1], opt);
								}
								paras.push(tmp);
							}
							break;
						}
					}
					catch (e) {}
				}
			}
			if(!replaced) {
				//if(window.console) window.console.log(para);
				if(!ioParaphrases[taskFrame]) ioParaphrases[taskFrame] = [];
				ioParaphrases[taskFrame].push(para);
			}
		}
	}
	return ioParaphrases;
};


TodoListParser.prototype.findMatch_ordered = function(text, paraphrase, task) {
	var i = -1;
	var n = -1;

	var move_to_next_token = true;
	var move_to_next_phrase = true;
	var failed_match = false;
	var invalid_paraphrase = false;

	var nconm = 0;
	var nargm = 0;
	var argument_bindings = {};

	var cur_token;
	var next_token;
	var cur_word;
	var cur_phrase;

	var words = text.split(/\s+/);

	while(1) {
		if(move_to_next_token) {
			i++;
			cur_token = paraphrase.getToken(i);
			next_token = paraphrase.getToken(i+1);
			move_to_next_token = false;
		}

		if(move_to_next_phrase) {
			n++;
			cur_word = words[n];
			cur_phrase = cur_word;
			move_to_next_phrase = false;
		}

		if(!cur_token) break;

		// FIXME: Currently cannot match with two consecutive arg tokens 
		if(cur_token.type() == "arg" && next_token && next_token.type() == "arg") {
			failed_match = true;
			invalid_paraphrase = true;
			break;
		}

		if(cur_token.type() == "const") {
			if(cur_phrase && cur_phrase.toLowerCase() == cur_token.name().toLowerCase()) {
				move_to_next_phrase = true;
				move_to_next_token = true;
				nconm++;
			}
			else {
				failed_match = true;
				break;
			}
		}
		else if(cur_token.type() == "arg") {
			var next_word = words[n+1];
			//if(window.console) window.console.log("Next Word : next_word");
			if(!next_word) {
				if(next_token) {
					failed_match = true;
					break;
				}
				argument_bindings[cur_token.name()] = cur_phrase;
				nargm++;
				break;
			}
			else if( next_token && next_word.toLowerCase() == next_token.name().toLowerCase() ) {
				argument_bindings[cur_token.name()] = cur_phrase;
				move_to_next_phrase = true;
				move_to_next_token = true;
				nargm++;
			}
			else {
				n++;
				cur_phrase += " "+next_word;
			}
		}
	}

	if(!failed_match) {
		var ttxt = task ? task.text() : text;
		var mbReg = /\+([a-zA-Z0-9_]+)\=([a-zA-Z0-9_:\-]+)\b/;
		var mb = mbReg.exec(ttxt);
		while(mb) {
			ttxt = ttxt.replace("+"+mb[1]+"="+mb[2], "+"+mb[1]);
			argument_bindings[mb[1]] = mb[2];
			mb = mbReg.exec(ttxt);
		}

		var rank = 0;
        for(var arg in argument_bindings) 
            if(argument_bindings[arg]) rank++;
		rank += 2*nconm;

		var matched_task = new MatchedTask(ttxt, rank, paraphrase.text(), argument_bindings);
		return matched_task;
	}
	return null;
};

TodoListParser.prototype.findMatch_regex = function(text, paraphrase, task) {
	var regex = paraphrase.regex;
	var args = paraphrase.args;

	var m = text.match(regex);
	if(m) {
		var bindings = {};
		for(var j=1; j<m.length; j++) {
			bindings[args[j-1]] = m[j];
		}
		//if(window.console) window.console.log(bindings);
		var ttxt = task ? task.text() : text;
		var mbReg = /\+([a-zA-Z0-9_]+)\=([a-zA-Z0-9_:\-]+)\b/;
		var mb = mbReg.exec(ttxt);
		while(mb) {
			ttxt = ttxt.replace("+"+mb[1]+"="+mb[2], "+"+mb[1]);
			bindings[mb[1]] = mb[2];
			mb = mbReg.exec(ttxt);
		}
		var rank = 0;
		for(var i=0; i< paraphrase.tokens().length; i++) {
			var tok = paraphrase.getToken(i);
			if(tok.type()=="arg") rank++;
			if(tok.type()=="const") rank+= 2;
		}
		var matched_task = new MatchedTask(ttxt, rank, paraphrase.text(), bindings);
		return matched_task;
	}
	return null;
};


TodoListParser.prototype.findMatchesForList = function(text, list) {
	if (!list) return undefined;
	if (!text) return undefined;

	text = text.replace(/\s*([,\.])\s*/g, " $1 ");
	text = text.replace(/[\(\)]/g, "");
	text = text.trim();

	var matches = new TodoItemMatches(text);
	var matched = false;

	var tasks = list.tasks();
	var matched_tasks = [];

	for (var ti=0; ti<tasks.length; ti++) {
		var task = tasks[ti];

		var paraphrases = task.paraphrases();

		for(var pi=0; pi<paraphrases.length; pi++) {
			var paraphrase = paraphrases[pi];

			var matchedTask = null;
			if(this._matching_algo == "ordered") {
				matchedTask = this.findMatch_ordered(text, paraphrase, task);
			}
			else if(this._matching_algo == "regex") {
				matchedTask = this.findMatch_regex(text, paraphrase, task);
			}
			if(matchedTask) {
				matched = true;
				matched_tasks.push(matchedTask);
			}
		}
	}

	// Pick the highest ranked match(es) for this task (if top_matches_only is passed in)
	if(matched_tasks.length > 0) {
		var sorted_matched_tasks = matched_tasks.sort (function(a,b) {return b.matchrank() - a.matchrank();} );
		var toptask = sorted_matched_tasks[0];
		matches.addMatchedTask(toptask);
		for (var i=1; i<sorted_matched_tasks.length; i++) {
			var mtask = sorted_matched_tasks[i];
			if(this._top_matches_only) {
				if(mtask.matchrank() == toptask.matchrank() && !mtask.equals(toptask)) {
					matches.addMatchedTask(mtask);
				} 
				else break; 
			}
			else {
				matches.addMatchedTask(mtask);
			}
      }
		return matches;
   }
   return undefined;
};



TodoListParser.prototype._tableContainsRow = function(table, trow) {
	for(var i=0; i<table.length; i++) {
		var row = table[i].row;
		var matches = true;
		for(var j=0; j<row.length; j++) 
			if(row[j] != trow[j]) matches = false;
		for(var j=0; j<trow.length; j++) 
			if(row[j] != trow[j]) matches = false;
		if(matches) return true;
	}
	return false;
};

TodoListParser.prototype.getOrderedTexts = function(utterance) {
	if(!utterance) return [];

	var table = [{row:[utterance], rank:0}];

	var iend = 1;
	for(var i=0; i<iend; i++) {
		var row = table[i].row;
		var rank = table[i].rank;
		for(var j=0; j<row.length; j++) {
			var txt = row[j];
			var m = this.findMatchesForList(txt, this._st_control_list);
			if(m) {
				var len = m._matched_tasks.length;
				for(var k=0; k<len; k++) {
					var t = m._matched_tasks[k];
					var p1 = t._argbindings['proc1'];
					var p2 = t._argbindings['proc2'];
					if(!p1 && !p2) continue;

					var trow = [];
					for(var x=0; x<row.length; x++) {
						if(x > j && p1 && p2) {
							trow[x+1] = row[x];
						}
						else if(x == j) {
							if(p1) trow[x] = p1;
							if(p2) trow[x+1] = p2;
						}
						else {
							trow[x] = row[x];
						}
					}
					if(!this._tableContainsRow(table, trow)) {
						table[iend] = {};
						table[iend].row = trow;
						table[iend].rank = rank + t._matchrank;
						iend++;
					}
				}
			}
		}
	}
	table.sort (function(a,b) {return b.rank - a.rank;});	
	return table;
};


TodoListParser.prototype.getNormalizedMatches = function(matches) {
	var nmatches = [];
	for(var i=0; i<matches.length; i++) {
		var row = matches[i].row;
		var rank = matches[i].rank;
		var curm = [{rank:rank, tasks:[]}];
		var rank = 0;
		for(var j=0; j<row.length; j++) {
			if(!row[j]) continue;
			var tasks = row[j]._matched_tasks;
			var newm = [];
			while(curm.length) {
				var cm = curm.pop();
				for(var k=0; k<tasks.length; k++) {
					var tasksdup = cm.tasks.concat(); // duplicate
					tasksdup.push(tasks[k]);
					var rank = cm.rank + tasks[k]._matchrank;
					newm.push({tasks:tasksdup, rank:rank});
				}
			}
			curm = newm;
		}
		nmatches = nmatches.concat(curm);
	}
	nmatches.sort (function(a,b) {return b.rank - a.rank;});	
	return nmatches;
};


TodoListParser.prototype.findDataBindingsForText = function(txt) {
	var bindings = [];
	if(!txt) return bindings;
	var dm = this.findMatchesForList(txt, this._d_control_list);
	if(dm) {
		for(var l=0; l<dm._matched_tasks.length; l++) {
			var dt = dm._matched_tasks[l];
			var dtname = dt._taskname.replace(/\s+\+.*/,'');
			bindings.push({type:dtname, name:dt._argbindings['d'], rank:dt._matchrank});
		}
	}
	return bindings;
};

TodoListParser.prototype.cloneMatch = function(m) {
	var mt = {
		_argbindings:{}, 
		_databindings:{}, 
		_component:m._component,
		_matchrank:m._matchrank,
		_paraphrase:m._paraphrase,
		_taskname:m._taskname
	};
	for(var arg in m._argbindings) {
		mt._argbindings[arg] = m._argbindings[arg];
	}
	for(var arg in m._databindings) {
		var db = m._databindings[arg];
		mt._databindings[arg] = (db && db.concat) ? db.concat() : db;
	}
	return mt;
};

TodoListParser.prototype.splitCompoundMatches = function(matches) {
	for(var i=0; i<matches.length; i++) {
		var ntasks = [];
		var tasks = matches[i].tasks;
		for(var j=0; j<tasks.length; j++) {
			if(!tasks[j]) continue;
			var task = tasks[j];
			if(task._taskname.match(/\|/)) {
				var txts = task._taskname.split(/\s*\|\s*/);
				for(var k=0; k<txts.length; k++) {
					var taskdup = this.cloneMatch(task);
					var tmp = new Paraphrase(txts[k]);
					var newbindings = {};
					for(var l=0; l<tmp.tokens().length; l++) {
						var token = tmp.getToken(l);
						if(token.type()=='arg' && task._argbindings[token.name()]) {
							newbindings[token.name()] = task._argbindings[token.name()];
						}
					}
					taskdup._taskname = txts[k];
					taskdup._argbindings = newbindings;
					ntasks.push(taskdup);
				}
			}
			else {
				ntasks.push(task);
			}
		}
		matches[i].tasks = ntasks;
	}
	return matches;
};


TodoListParser.prototype.normalizeDataMatches = function(matches) {
	var nmatches = [];
	for(var i=0; i<matches.length; i++) {
		var tasks = matches[i].tasks;
		var mrank = matches[i].rank;

		var curm = [{rank:mrank, tasks:[]}];

		for(var j=0; j<tasks.length; j++) {
			var task = tasks[j];
			if(!task) continue;

			var task_bindings = [{rank:0, bindings:{}}];

			for(var arg in task._databindings) {
				var dbs = task._databindings[arg];
				var newm = [];
				while(task_bindings.length) {
					var cm = task_bindings.pop();
					for(var k=0; k<dbs.length; k++) {
						var datadup = {};
						for(var xd in cm.bindings) 
							datadup[xd] = cm.bindings[xd];
						datadup[arg] = dbs[k];
						var rank = cm.rank + dbs[k].rank;
						newm.push({bindings:datadup, rank:rank});
					}
				}
				task_bindings = newm;
			}
			
			var newm = [];
			while(curm.length) {
				var cm = curm.pop();
				/*if(!task._taskname.match(/^C_/)) {
					var tasksdup = cm.tasks.concat(); // duplicate
					tasksdup.push(this.cloneMatch(task));
					newm.push({tasks:tasksdup, rank:cm.rank});
					continue;
				}*/
				for(var k=0; k<task_bindings.length; k++) {
					var db = task_bindings[k];
					var tasksdup = cm.tasks.concat(); // duplicate
					var mt = this.cloneMatch(task);
					mt._databindings = db.bindings;
					var rank = cm.rank + db.rank;
					tasksdup.push(mt);
					newm.push({tasks:tasksdup, rank:rank});
				}
			}
			curm = newm;
		}
		if(curm.length) nmatches = nmatches.concat(curm);
	}
	nmatches.sort (function(a,b) {return b.rank - a.rank;});	
	return nmatches;
};

TodoListParser.prototype.normalizeComponentMatches = function(matches) {
	var nmatches = [];
	for(var i=0; i<matches.length; i++) {
		var tasks = matches[i].tasks;
		var mrank = matches[i].rank;

		var curm = [{rank:mrank, tasks:[]}];

		for(var j=0; j<tasks.length; j++) {
			var task = tasks[j];
			if(!task) continue;
			var newm = [];
			while(curm.length) {
				var cm = curm.pop();
				if(!task._taskname.match(/^C_/)) {
					var tasksdup = cm.tasks.concat(); // duplicate
					tasksdup.push(task);
					newm.push({tasks:tasksdup, rank:cm.rank});
					continue;
				}
				for(var k=0; k<task._component.length; k++) {
					var c = task._component[k];
					var tasksdup = cm.tasks.concat(); // duplicate
					var mt = this.cloneMatch(task);
					mt._component = c;
					tasksdup.push(mt);
					newm.push({tasks:tasksdup, rank:cm.rank});
				}
			}
			curm = newm;
		}
		if(curm.length) nmatches = nmatches.concat(curm);
	}
	nmatches.sort (function(a,b) {return b.rank - a.rank;});	
	return nmatches;
};



TodoListParser.prototype.findPropertyDataBindings = function(dtxt) {
	var nbindings = [];
	var pm = this.findMatchesForList(dtxt, this._p_control_list);
	if(pm) {
		for(var k=0; k<pm._matched_tasks.length; k++) {
			var pt = pm._matched_tasks[k];
			var pdtxt = pt._argbindings['d'];
			var bindings = this.findDataBindingsForText(pdtxt);
			for(var l=0; l<bindings.length; l++) {
				bindings[l].prop = pt._argbindings['p'];
				bindings[l].val = pt._argbindings['val'];
				bindings[l].rank += pt._matchrank;
			}
			nbindings = nbindings.concat(bindings);
		}
	}
	return nbindings;
};

// Get all data/property bindings
// -- We don't check validity of datatypes, properties etc at this stage
TodoListParser.prototype.findDataMatches = function(matches) {
	for(var i=0; i<matches.length; i++) {
		var tasks = matches[i].tasks;
		for(var j=0; j<tasks.length; j++) {
			var t = tasks[j];
			t._databindings = {};
			for(var key in t._argbindings) {
				if(typeof(key) != 'string') continue;
				var dtxt = t._argbindings[key];
				if(key.match(/^(ip|op|v)/)) {
					if(dtxt) {
						var bindings = [];
						bindings = bindings.concat(this.findPropertyDataBindings(dtxt));

						if(!bindings.length)
							bindings = bindings.concat(this.findDataBindingsForText(dtxt));

						// Re-evaluate property bindings for data afterwards as well 
						// - In case adjective type of phrases are used (ex: a collection of normalized data)
						var nbindings = bindings.concat(); // Duplicate
						while(bindings.length) {
							var binding = bindings.pop();
							if(!binding.prop || !binding.val) {
								var tbindings = this.findPropertyDataBindings(binding.name);
								for(var l=0; l<tbindings.length; l++) {
									if(tbindings[l].prop && tbindings[l].val) {
										tbindings[l].type = binding.type;
										tbindings[l].rank += binding.rank;
										nbindings.push(tbindings[l]);
									}
								}
							}
						}

						t._databindings[key] = nbindings;
					}
				}
				/*else if(key.match(/^v/)) {
					t._databindings[key] = {name:dtxt, rank:0};
				}*/
				else if(key == 'c') {
					t._component = dtxt;
				}
			}
			//if(window.console) window.console.log(t._databindings);
		}
	}
	return matches;
};

TodoListParser.prototype.findMatches = function(text) {
	// Match with statement paraphrases
	var table = this.getOrderedTexts(text);
		
	// Match with component paraphrases
	var matches = [];
	for(var i=0; i<table.length; i++) {
		var row = table[i].row;
		var mrow = [];
		for(var j=0; j<row.length; j++) {
			mrow.push(this.findMatchesForList(row[j], this._c_control_list));
		}
		matches[i] = {rank:table[i].rank, row:mrow};
	}

	var nmatches = this.getNormalizedMatches(matches);
	var cmatches = this.splitCompoundMatches(nmatches);
	var dmatches = this.findDataMatches(cmatches);
	var ndmatches = this.normalizeDataMatches(dmatches);

	ndmatches.sort (function(a,b) {return b.rank - b.tasks.length - a.rank + a.tasks.length;});	

	if(this._top_matches_only && ndmatches.length) {
		var tmp = [];
		var toprank = ndmatches[0].rank - ndmatches[0].tasks.length;
		for(var i=0; i<ndmatches.length; i++) {
			var match = ndmatches[i];
			if(match.rank - match.tasks.length == toprank)
				tmp.push(match);
			else
				break;
		}
		ndmatches = tmp;
	}

	//if(window.console) window.console.log(text);
	return ndmatches;
};

TodoListParser.prototype.getPossibleSentences = function(text, cutoff) {
	//return this._c_trie.getWordsForPrefix(text)
	return this._st_trie.getWordsForPrefix(text);
	/*for(var i=0; i<text.length; i++) {
		var c = text[i];
	}*/
};

TodoListParser.prototype.getTrieForList = function(list, trie_type) {
	// Create a Trie
	var trie = new TrieNode(this, trie_type, this.getTrieForArg);
	var tasks = list.tasks();
	for(var i=0; i<tasks.length; i++) {
		var ps = tasks[i].paraphrases();
		for(var j=0; j<ps.length; j++) {
			trie.add(ps[j].text());
		}
	}
	//if(window.console) window.console.log(trie);
	return trie;
};

//io:0, c:1, d:2, prop:3, val:4, v:5, proc:6, misc:99
TodoListParser.prototype.getTrieForArg = function(argtype) {
	if(argtype == 6) return this._c_trie;
	if(argtype == 0) return this._p_trie;
	if(argtype == 2) return this._d_trie;
};


TodoListParser.prototype.isEnd = function(text) {
	var endings = {"the end":1, "thats it":1, "that's it":1, "done":1};
	return endings[text.toLowerCase()];
};
