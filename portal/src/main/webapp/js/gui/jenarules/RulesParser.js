var RulesParser = function(text) {
	this.ruletypes = this.getRuleTypes();
	this.rules = this.parseAllRules(text);
};


RulesParser.prototype.getRuleTypes = function() {
	var arr = {};
	for(var fid in R.FT) {
		var rt = R.FT[fid];
		arr[rt.name()] = rt;
		arr[rt.name()+rt.numargs()] = rt;
	}
	return arr;
};

// Parse all rules text
RulesParser.prototype.parseAllRules = function(text) {
	var rules = [];
	var currule = '';
	var lines = text.split(/\n/);
	var ruleRe = /\[(.+?):(.+?)\]/;
	for(var i=0; i<lines.length; i++) {
		var line = lines[i];
		line = line.trim();
		line = line.replace("http://www.w3.org/2001/XMLSchema#", "xsd:");
		line = line.replace(/#.*/, '');
		if(!line) continue;
		currule += line;
		var arr = ruleRe.exec(currule);
		if(arr != null) {
			// Found a rule
			var rulename = arr[1].trim();
			var ruletext = arr[2].trim();

			// Parse this rule
			var rule = this.parseRule(ruletext);
			//rule.name(rulename);
			//console.log(rule);
			console.log(rule.toString());
			rules.push(rule);

			currule = '';
		}
	}
	return rules;
};

// Move the following functions into the Rule Class
// Rule (comp, type, preconditions, effects)
// - Subclass: Propagation, Invalidation, Configuration
// RuleFunctionType (name, numargs, arg types, return type)
// RuleFunction (fntype, args)
RulesParser.prototype.parseRule = function(ruletext) {
	var arr = ruletext.split(/\s*->\s*/);
	var lhs = arr[0];
	var rhs = arr[1];

	var cvar = '';
	var cname = '';
	var cinvars = [];
	var coutvars = [];
	var varidmap = {};
	var preconds = [];

	var condRe = /\s*.*?\(.+?\)\s*/g;

	// Look at the Left hand side (i.e. preconditions) of the rule
	var litems = lhs.match(condRe);
	for(var j=0; j<litems.length; j++) 
		litems[j] = litems[j].trim().replace(/^,\s*/, '');
	
	if(litems != null) {
		// Parse all items
		var parseditems = [];
		for(var j=0; j<litems.length; j++) {
			var ret = this.parseRuleItem(litems[j]);
			parseditems.push(ret);
		}

		// First get the component variable and type
		for(var j=0; j<parseditems.length; j++) {
			var t = parseditems[j];
			if(t instanceof Triple) {
				if(t.p() == "rdf:type") {
					var m = /(.+):(.+)Class/.exec(t.o());
					if(m != null) {
						if(m[1]=='pcdom'||m[1]=='acdom'||m[1]=='pc'||m[1]=='ac') {
							cvar = t.s();
							cname = m[2];
						}
					}
				}
			}
		}

		// Then get the I/O variables
		for(var j=0; j<parseditems.length; j++) {
			var t = parseditems[j];
			if(t instanceof Triple) {
				if((t.s()==cvar)&&((t.p()=="pc:hasInput")||(t.p()=="ac:hasInput"))) {
					varidmap[t.o()] = new RuleInput();
					cinvars.push(t.o());
				}
				else if((t.s()==cvar)&&((t.p()=="pc:hasOutput")||(t.p()=="ac:hasOutput"))) {
					varidmap[t.o()] = new RuleOutput();
					coutvars.push(t.o());
				}
			}
		}

		// Map I/O Variables to Argument IDs
		for(var j=0; j<parseditems.length; j++) {
			var t = parseditems[j];
			if(t instanceof Triple) {
				if((t.p()=="pc:hasArgumentID")||(t.p()=="ac:hasArgumentID")) {
					var argid = t.o().replace(/"/g, '');
					// If the argid is a variable as well
					if(argid.charAt(0)=='?') {
						varidmap[argid] = new RuleArgProp(varidmap[t.s()], "__ARGID");
					}
					else {
						var argio = varidmap[t.s()];
						if(argio) argio.arg(argid);
					}
				}
			}
		}

		// Get any variables defined for metric properties
		for(var j=0; j<parseditems.length; j++) {
			var t = parseditems[j];
			if(t instanceof Triple) {
				if(t.p()=='rdfs:subPropertyOf') {
					if(t.o()=='dc:hasMetrics') 
						varidmap[t.s()] = '__OBJ_PROP';
					if(t.o()=='dc:hasDataMetrics') 
						varidmap[t.s()] = '__DATA_PROP';
				}
			}
		}

		// Get property value bindings
		for(var j=0; j<parseditems.length; j++) {
			var t = parseditems[j];
			if(t instanceof Triple) {
				if(t.o().charAt(0)=='?') {
					var v = varidmap[t.s()];
					if(!v) continue;
					// Get property
					var dcprop = this.parseProperty(t.p(), varidmap);
					if(!dcprop) continue;
					varidmap[t.o()] = new RuleArgProp(v, dcprop);
				}
			}
		}

		// Then get any Preconditions
		for(var j=0; j<parseditems.length; j++) {
			var t = parseditems[j];
			if(t instanceof Triple) {
				// If the object isn't a variable (i.e. doesn't begin with a '?')
				// - It is a precondition (otherwise it be a variable binding)
				if(t.o().charAt(0)!='?') {
					var dcprop = this.parseProperty(t.p(), varidmap);
					if(!dcprop) continue;

					var v = varidmap[t.s()];
					if(!v) continue;
					var val = this.parseLiteral(t.o());

					var precond = new Triple(v, dcprop, val);
					preconds.push(precond);
					//valcheck = "var.dcprop";
					//preconds[valcheck] = val;
				}
			}
		}

		// Finally check the Functions: Get preconditions or value bindings
		for(var j=0; j<parseditems.length; j++) {
			var ret = parseditems[j];
			if(ret instanceof RuleFunction) {
				var fnargs = ret.args();
				var args = [];
				var addprecond = true;
				for(var l=0; l<fnargs.length; l++) {
					var arg = fnargs[l];
					if(arg.charAt(0)=='?') {
						// This argument is a variable
						var barg = varidmap[arg];

						// If this is the last item in the function and the variable isn't bound 
						// then bind it to the value of function
						if(!barg && l==(fnargs.length-1)) {
							barg = arg;
							var bound_fnargs = [];
							for(var m=0; m<fnargs.length-1; m++) {
								var narg = fnargs[m];
								if(narg.charAt(0)=='?' && varidmap[narg])
									narg = varidmap[narg];
								else
									narg = this.parseLiteral(narg);
								bound_fnargs.push(narg);
							}
							// Add value binding
							varidmap[arg] = new RuleFunction(ret.fn(), bound_fnargs); 
							addprecond = false;
						}
						else if(!barg) {
							addprecond = false;
							barg = arg;
						}
						args.push(barg);
					}
					else if(ret.fn().name()=='noValue') {
						if(l==1)
							args.push(this.parseProperty(arg, varidmap));
						else if(l==2)
							args.push(this.parseLiteral(arg));
					}
					else {
						args.push(arg);
					}
				}
				// Add precondition
				if(addprecond)
					preconds.push(new RuleFunction(ret.fn(), args));
			}
		}
	}

	// Add preconditions to rule
	var rule = new Rule(cname, null, preconds, []);

	// Now Look at the Right hand side (i.e. effects) of the rule
	var ritems = rhs.match(condRe);
	for(var j=0; j<ritems.length; j++) 
		ritems[j] = ritems[j].trim().replace(/^,\s*/, '');

	if(ritems != null) {
		for(var j=0; j<ritems.length; j++) {
			var item = ritems[j];
			t = this.parseRuleItem(item);
			if(t instanceof Triple) {
				if((t.s()==cvar)&&((t.p()=="pc:isInvalid")||(t.p()=="ac:isInvalid"))) {
					if((t.o().toLowerCase()=='"true"^^xsd:boolean')
						|| (t.o().toLowerCase()=="'true'^^xsd:boolean"))
						rule = this.addInvalidationRule(rule);
				}
				else if(this.inArray(t.s(), cinvars) || this.inArray(t.s(), coutvars)) {
					if((t.p()=="pc:hasValue")||(t.p()=="ac:hasValue")) {
						rule = this.addParamConfigRule(rule, t, varidmap);
					}
					else if((t.p()=="pc:hasDimensionSizes")||(t.p()=="ac:hasDimensionSizes")) {
						rule = this.addDimensionRule(rule, t, varidmap);
					}
					else if((t.p()=="pc:hasDimensionIndices")||(t.p()=="ac:hasDimensionIndices")) {
						rule = this.addDimensionRule(rule, t, varidmap);
					}
					else {
						rule = this.addPropagationRule(rule, t, varidmap);
					}
				}
			}
		}
	}

	if(!rule.type()) {
		rule.type('MISC');
	}
	//rule['lhs'] = litems;
	//rule['rhs'] = ritems;

	return rule;
};


RulesParser.prototype.parseRuleItem = function(item) {
	var fns = [];
	var triples = [];
	if((m = /^(.+?)\((.+)\)/.exec(item)) !== null) {
		fn = m[1].trim();
		args = m[2].trim().split(/\s*[, ]\s*/);
		for(var i=0; i<args.length; i++) {
			if(!args[i]) {
				args.splice(i, 1);
				i--;
			}
		}
		var fntype = this.ruletypes[fn];
		if(fntype) {
			if(fntype.returntype() == R.AT.BOOL) 
				fntype = this.ruletypes[fn+args.length];
			else {
				fntype = this.ruletypes[fn+(args.length-1)];
				if(!fntype) fntype = this.ruletypes[fn+'-1'];
			}
			if(fntype)
				return new RuleFunction(fntype, args);
		}
	}
	else if((m = /^\((.+?)\s+(.+?)\s+(.+?)\s*\)/.exec(item)) !== null) {
		var var1 = m[1];
		var prop = m[2];
		var var2 = m[3];
		return new Triple(var1, prop, var2);
	}
};


RulesParser.prototype.parseLiteral = function(val) {
	if(((n = /"(.+?)"\^\^xsd:(.+)/.exec(val)) !== null) ||
	   ((n = /'(.+?)'\^\^xsd:(.+)/.exec(val)) !== null)) {
		val = n[1];
		/*if(n[2] == "boolean" || n[2] == "bool") {
			if(val && val.toLowerCase() !== 'false') val = true;
			else val = false;
		}
		else if(n[2] == "int" || n[2] == "integer") {
			val = parseInt(val);
		}
		else if(n[2] == "float" || n[2] == "double") {
			val = parseFloat(val);
		}*/
		val = '"'+val+'"';
	}
	else if((n = /"(.+)"/.exec(val)) !== null) {
		//val = n[1];
	}
	else if((n = /'(.+)'/.exec(val)) !== null) {
		val = '"'+n[1]+'"';
	}
	else if((n = /(.+):(.+)/.exec(val)) !== null) {
		val = n[2];
	}
	return val;
};

RulesParser.prototype.parseProperty = function(prop, varidmap) {
	if((m = /dcdom:(.+)/.exec(prop)) !== null)
		return m[1];
	else if(prop=='rdf:type') 
		return '__TYPE';
	else if(prop=='ac:hasValue' || prop=='pc:hasValue') 
		return '__VALUE';
	else if(prop[0]=='?')
		return varidmap[prop];
	return null;
};

RulesParser.prototype.addPropagationRule = function(rule, triple, varidmap) {
	var dcprop = this.parseProperty(triple.p(), varidmap);
	if(dcprop) {
		var tovar = varidmap[triple.s()];
		var fromvar = '';
		if(triple.o().charAt(0)=='?')
			fromvar = varidmap[triple.o()];
		else 
			fromvar = this.parseLiteral(triple.o());
		if(fromvar != null && tovar != null ) {
			rule.type("__METAPROP");
			topropvar = new RuleArgProp(tovar, dcprop); 
			rule.add_effect(new RuleEffect(topropvar, fromvar));
		}
	}
	return rule;
};

RulesParser.prototype.inArray = function(needle, haystack) {
	for(var i=0; i<haystack.length; i++)
		if(haystack[i] == needle) 
			return true;
	return false;
};

RulesParser.prototype.addParamConfigRule = function(rule, triple, varidmap) {
	rule.type("__PARAMCONFIG");
	var v = varidmap[triple.s()];
	if(triple.o().charAt(0)=='?')
		value = varidmap[triple.o()];
	else
		value = this.parseLiteral(triple.o());
	rule.add_effect(new RuleEffect(v, value));
	return rule;
};

RulesParser.prototype.addDimensionRule = function(rule, triple, varidmap) {
	if((m = /hasDimension(.+)/.exec(triple.p())) !== null) {
		var sx = m[1].toLowerCase();
		var v = varidmap[triple.s()];
		var value = varidmap[triple.o()];
		rule.type("__COLLCONFIG");
		rule.add_effect(new RuleEffect(v, value, sx));
	}
	return rule;
};

RulesParser.prototype.addConditionsToRule = function(rule, preconds) {
	var filtered_pcs = [];
	for(var i=0; i<preconds.length; i++) {
		var pc = preconds[i];
		if(pc.fn()) {
			if(pc.fn() == R.FT.PRINT) continue;
			//if(pc.fn()==R.FT.NOVALUE && pc.args().length==2) continue;
		}
		filtered_pcs.push(pc);
	}
	if(filtered_pcs.length)
		rule.preconditions(filtered_pcs);
	return rule;
};

RulesParser.prototype.addInvalidationRule = function(rule) {
	if(rule.preconditions().length) 
		rule.type("__PRECON_INV");
	return rule;
};


