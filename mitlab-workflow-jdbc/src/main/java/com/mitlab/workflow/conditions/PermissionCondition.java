package com.mitlab.workflow.conditions;

import java.util.Map;

import com.mitlab.workflow.Condition;
import com.mitlab.workflow.WorkflowUser;

public class PermissionCondition implements Condition {
	private static final long serialVersionUID = 1L;

	@Override
	public boolean passesCondition(Map<String, Object> inputs, Map<String, Object> args, WorkflowUser caller) {
		return caller.hasAuthority((String) args.get("permission"));
	}

}
