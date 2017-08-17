package com.mitlab.workflow.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mitlab.workflow.Function;
import com.mitlab.workflow.UserGroup;
import com.mitlab.workflow.Workflow;
import com.mitlab.workflow.WorkflowUser;
import com.mitlab.workflow.descriptor.ResultDescriptor;

public class TestStepUserFunction implements Function {
	private static final long serialVersionUID = 1L;

	@Override
	public void execute(Map<String, Object> inputs, Map<String, Object> args) {
		List<UserGroup> userGroupList = new ArrayList<UserGroup>();
		String group = (String) args.get("group");
		if (group == null) {
			group = ((ResultDescriptor) args.get(Workflow.ARG_RESULT_DESCRIPTOR)).getUserGroup();
		}
		WorkflowUser caller = (WorkflowUser) args.get(Workflow.ARG_ACTION_CALLER);
		userGroupList.add(new UserGroup(caller.getUsername(), group));
		userGroupList.add(new UserGroup(caller.getUsername(), group + 'X'));
		args.put(Workflow.ARG_LOADED_USER_GROUP, userGroupList);
	}

}
