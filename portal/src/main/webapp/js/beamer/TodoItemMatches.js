var TodoItemMatches = function(text, matched_tasks) {
	this._text          = text;
	this._matched_tasks = matched_tasks ? matched_tasks : [];
};

TodoItemMatches.prototype.addMatchedTask = function(matched_task) {
   this._matched_tasks.push(matched_task);
};


TodoItemMatches.prototype.text = function(text) {
   if(text) this._text = text;
	else return this._text;
};

TodoItemMatches.prototype.matched_tasks = function(matched_tasks) {
   if(matched_tasks) this._matched_tasks = matched_tasks;
	else return this._matched_tasks;
};
