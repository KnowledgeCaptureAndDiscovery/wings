var RuleFunction = function(fn, args) {
	this._fn = fn;
	this._args = args;
};

RuleFunction.prototype.fn = function(fn) {
   if(fn) this._fn = fn;
	else return this._fn;
};

RuleFunction.prototype.add_arg = function(arg) {
	this._args.push(arg);
};

RuleFunction.prototype.args = function(args) {
   if(args) this._args = args;
	else return this._args;
};

RuleFunction.prototype.toString = function() {
	var fname = this._fn.name();
	return fname+" ( "+this._args.join(", ")+" )";
};
