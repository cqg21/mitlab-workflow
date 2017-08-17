package com.mitlab.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum FlowStatus {
	UNKNOW("unknow"), CREATED("created"), COMPLETED("completed"), KILLED("killed"), SUSPENDED("suspended"), ACTIVATED("activated");

	private final String value;
	private static final Map<String, FlowStatus> STATUS_MAPPING = new ConcurrentHashMap<String, FlowStatus>();
	static {
		STATUS_MAPPING.put(UNKNOW.value, UNKNOW);
		STATUS_MAPPING.put(CREATED.value, CREATED);
		STATUS_MAPPING.put(COMPLETED.value, COMPLETED);
		STATUS_MAPPING.put(KILLED.value, KILLED);
		STATUS_MAPPING.put(SUSPENDED.value, SUSPENDED);
		STATUS_MAPPING.put(ACTIVATED.value, ACTIVATED);
	}

	private FlowStatus(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static FlowStatus value(String value) {
		FlowStatus status = STATUS_MAPPING.get(value);
		if (status != null) {
			return status;
		}
		throw new IllegalArgumentException("Unexpected WorkflowStatus:" + value);
	}

	public String toString() {
		return this.value;
	}
}
