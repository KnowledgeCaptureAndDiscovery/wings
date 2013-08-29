// ----------------------------------------
var RuleArgProp = function(arg, prop) {
	this._arg = arg;
	this._prop = prop;
};
RuleArgProp.prototype.arg = function(arg) {
   if(arg) this._arg = arg;
	else return this._arg;
};
RuleArgProp.prototype.prop = function(prop) {
   if(prop) this._prop = prop;
	else return this._prop;
};
RuleArgProp.prototype.toString = function() {
	return this._arg.toString()+"."+this._prop.toString();
};
// ----------------------------------------


// ----------------------------------------
var RuleEffect = function(v, val, type) {
	this._v = v;
	this._val = val;
	this._type = type;
};
RuleEffect.prototype.v = function(v) {
   if(v) this._v = v;
	else return this._v;
};
RuleEffect.prototype.val = function(val) {
   if(val) this._val = val;
	else return this._val;
};
RuleEffect.prototype.type = function(type) {
   if(type) this._type = type;
	else return this._type;
};
RuleEffect.prototype.toString = function() {
	return this._v.toString()+(this._type? "."+this._type :"")+" = "+this._val.toString();
};
// ----------------------------------------


// ----------------------------------------
var RuleArg = function(arg) {
	this._arg = arg ? arg : '__ANY';
};
RuleArg.prototype.arg = function(arg) {
	if(arg) this._arg = arg;
	else return this._arg;
};
RuleArg.prototype.toString = function() { return this._arg.toString(); };
// ----------------------------------------
var RuleInput = function(arg) { RuleArg.call(this, arg); };
RuleInput.prototype = Object.create(RuleArg.prototype);
RuleInput.prototype.constructor = RuleInput;
RuleInput.prototype.toString = function() { return 'Inputs.'+this._arg.toString(); };
// ----------------------------------------
var RuleOutput = function(arg) { RuleArg.call(this, arg); };
RuleOutput.prototype = Object.create(RuleArg.prototype);
RuleOutput.prototype.constructor = RuleOutput;
RuleOutput.prototype.toString = function() { return 'Outputs.'+this._arg.toString(); };
// ----------------------------------------

// ----------------------------------------
//var RuleArgID = function(arg) { RuleArg.call(this, arg); };
//RuleArgID.prototype = Object.create(RuleArg.prototype);
//RuleArgID.prototype.constructor = RuleArgID;
// ----------------------------------------
//var RuleArgValue = function(arg) { RuleArg.call(this, arg); };
//RuleArgValue.prototype = Object.create(RuleArg.prototype);
//RuleArgValue.prototype.constructor = RuleArgValue;
// ----------------------------------------
