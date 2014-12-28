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

package edu.isi.wings.common.logging;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class LogEvent implements LoggingKeys {

	private static String START_POSTFIX = ".start";
	private static String END_POSTFIX = ".end";

	private String _eventName;
	private String _progName;
	private Map<String, String> _entityIdMap;
	private String _eventId;

	public LogEvent(String eventName, String programName, String entityType, String entityId) {

		_eventName = eventName;
		_progName = programName;
		_eventId = eventName + "_" + UUID.randomUUID().toString();

		_entityIdMap = new HashMap<String, String>();
		_entityIdMap.put(entityType, entityId);

	}

	public LogEvent(String eventName, String programName, Map<String, String> entityTypeToIdMap) {
		_eventName = eventName;
		_progName = programName;
		_eventId = eventName + "_" + UUID.randomUUID().toString();
		_entityIdMap = entityTypeToIdMap;

	}

	public LogEvent(String eventName, String programName) {
		_eventName = eventName;
		_progName = programName;
		_eventId = eventName + "_" + UUID.randomUUID().toString();
		_entityIdMap = new HashMap<String, String>();
	}

	public EventLogMessage createStartLogMsg() {
		String msgid = UUID.randomUUID().toString();
		EventLogMessage elm = new EventLogMessage(_eventName + START_POSTFIX).add(MSG_ID, msgid)
				.add(EVENT_ID_KEY, _eventId).add(PROG, _progName);
		for (Map.Entry<String, String> entry : _entityIdMap.entrySet()) {
			elm.add(entry.getKey(), entry.getValue());
		}
		return elm;
	}

	public EventLogMessage createLogMsg() {
		String msgid = UUID.randomUUID().toString();
		EventLogMessage elm = new EventLogMessage(_eventName).add(MSG_ID, msgid).add(EVENT_ID_KEY,
				_eventId);
		for (Map.Entry<String, String> entry : _entityIdMap.entrySet()) {
			elm.add(entry.getKey(), entry.getValue());
		}
		return elm;

	}

	public EventLogMessage createEndLogMsg() {
		String msgid = UUID.randomUUID().toString();
		EventLogMessage elm = new EventLogMessage(_eventName + END_POSTFIX).add(MSG_ID, msgid).add(
				EVENT_ID_KEY, _eventId);
		for (Map.Entry<String, String> entry : _entityIdMap.entrySet()) {
			elm.add(entry.getKey(), entry.getValue());
		}
		return elm;

	}

	public static EventLogMessage createIdHierarchyLogMsg(String parentIdType, String parentId,
			String childIdType, Iterator<String> childIds) {
		String msgid = UUID.randomUUID().toString();
		String eventId = "idHierarchy_" + UUID.randomUUID().toString();
		EventLogMessage lm = new EventLogMessage("event.id.creation").add(MSG_ID, msgid)
				.add(EVENT_ID_KEY, eventId).add("parent.id.type", parentIdType)
				.add("parent.id", parentId);
		lm.add("child.ids.type", childIdType);
		StringBuffer cids = new StringBuffer("{");
		while (childIds.hasNext()) {
			cids.append(childIds.next());
			cids.append(",");
		}
		cids.append("}");
		lm.add("child.ids", cids.toString());
		return lm;
	}
}
