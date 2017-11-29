package com.mitlab.workflow;

import java.io.Serializable;
import java.util.Map;

public interface Condition extends Serializable {
	boolean passesCondition(Map<String, Object> inputs, Map<String, Object> args, WorkflowUser caller);
}
