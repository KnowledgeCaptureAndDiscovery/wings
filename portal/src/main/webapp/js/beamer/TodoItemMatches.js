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
