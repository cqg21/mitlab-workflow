package com.mitlab.workflow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum FlowPhase {
	ON_START("onStart"), ON_ACTION("onAction"), ON_COMPLETE("onComplete");

	private final String value;
	private static final Map<String, FlowPhase> STATUS_MAPPING = new ConcurrentHashMap<String, FlowPhase>();
	static {
		STATUS_MAPPING.put(ON_START.value, ON_START);
		STATUS_MAPPING.put(ON_ACTION.value, ON_ACTION);
		STATUS_MAPPING.put(ON_COMPLETE.value, ON_COMPLETE);
	}

	private FlowPhase(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

	public static FlowPhase value(String value) {
		FlowPhase status = STATUS_MAPPING.get(value);
		if (status != null) {
			return status;
		}
		throw new IllegalArgumentException("Unexpected WorkflowPhase:" + value);
	}

	public String toString() {
		return this.value;
	}
}
