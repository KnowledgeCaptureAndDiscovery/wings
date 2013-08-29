var Token = function ( name, type, arg_type, arg_cardinality ) {
	this._name            = name;
	this._type            = type  ? type : 'const';
	this._arg_type        = arg_type ? arg_type : undefined;
	this._arg_cardinality = arg_cardinality ? arg_type : undefined;
};


Token.prototype.name = function(name) {
   if(name) this._name = name;
	else return this._name;
};

Token.prototype.type = function(type) {
   if(type) this._type = type;
	else return this._type;
};

Token.prototype.arg_type = function(arg_type) {
   if(arg_type) this._arg_type = arg_type;
	else return this._arg_type;
};

Token.prototype.arg_cardinality = function(arg_cardinality) {
   if(arg_cardinality) this._arg_cardinality = arg_cardinality;
	else return this._arg_cardinality;
};
