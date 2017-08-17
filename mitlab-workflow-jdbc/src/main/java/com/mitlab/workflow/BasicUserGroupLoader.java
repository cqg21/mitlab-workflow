package com.mitlab.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BasicUserGroupLoader implements UserGroupLoader {

	@Override
	public List<UserGroup> loadUsers(Map<String, Object> inputs, Map<String, Object> args, String userGroup) {
		userGroup = userGroup == null ? "" : userGroup;
		String[] groups = userGroup.split(",");
		@SuppressWarnings("unchecked")
		List<UserGroup> userGroupList = (List<UserGroup>) args.get(Workflow.ARG_LOADED_USER_GROUP);
		if (userGroupList != null) {
			return userGroupList;
		}
		userGroupList = new ArrayList<UserGroup>();
		WorkflowUser caller = (WorkflowUser) args.get(Workflow.ARG_ACTION_CALLER);
		for (String group : groups) {
			userGroupList.add(new UserGroup(caller.getUsername(), group));
		}
		return userGroupList;
	}

}
