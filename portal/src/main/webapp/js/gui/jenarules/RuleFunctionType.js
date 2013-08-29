var RuleFunctionType = function(name, numargs, argtypes, returntype) {
	this._name = name;
	this._numargs = numargs;
	this._returntype = returntype;
	this._argtypes = argtypes;
};

RuleFunctionType.prototype.name = function(name) {
   if(name) this._name = name;
	else return this._name;
};

RuleFunctionType.prototype.numargs = function(numargs) {
   if(numargs) this._numargs = numargs;
	else return this._numargs;
};

RuleFunctionType.prototype.add_argtype = function(argtype) {
	this._argtypes.push(argtype);
};

RuleFunctionType.prototype.argtypes = function(argtypes) {
   if(argtypes)  this._argtypes = argtypes;
	else return this._argtypes;
};

RuleFunctionType.prototype.returntype = function(returntype) {
   if(returntype) this._returntype = returntype;
	else return this._returntype;
};

