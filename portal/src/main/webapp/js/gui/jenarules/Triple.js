var Triple = function(s, p, o) {
	this._s = s;
	this._p = p;
	this._o = o;
};

Triple.prototype.s = function(s) {
   if(s) this._s = s;
	else return this._s;
};

Triple.prototype.p = function(p) {
   if(p) this._p = p;
	else return this._p;
};

Triple.prototype.o = function(o) {
   if(o) this._o = o;
	else return this._o;
};

Triple.prototype.toString = function() {
	return "( "+this._s+" "+this._p+" "+this._o+" )";
};
