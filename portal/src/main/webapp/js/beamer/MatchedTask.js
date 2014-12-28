/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var MatchedTask = function(taskname, matchrank, paraphrase, argbindings ) {
	this._taskname    = taskname;
	this._matchrank   = matchrank;
	this._paraphrase  = paraphrase;
	this._argbindings = argbindings ? argbindings : {};
};


MatchedTask.prototype.taskname = function(taskname) {
	if(taskname) this._taskname = taskname;
	else return this._taskname;
};

MatchedTask.prototype.matchrank = function(matchrank) {
	if(matchrank) this._matchrank = matchrank;
	else return this._matchrank;
};

MatchedTask.prototype.paraphrase = function(paraphrase) {
	if(paraphrase) this._paraphrase = paraphrase;
	else return this._paraphrase;
};

MatchedTask.prototype.argbindings = function(argbindings) {
	if(argbindings) this._argbindings = argbindings;
	else return this._argbindings;
};

MatchedTask.prototype.equals = function(task) {
	//window.console.log(task.taskname()." : ".this.taskname());
	if ( task.taskname() != this.taskname() ) return false;
	var bindings = this.argbindings();
	var tbindings = task.argbindings();
	for(var key in tbindings) 
		if (tbindings[key] != bindings[key]) return false;
	for(var key in bindings) 
		if (bindings[key] != tbindings[key]) return false;
	return true;
};
