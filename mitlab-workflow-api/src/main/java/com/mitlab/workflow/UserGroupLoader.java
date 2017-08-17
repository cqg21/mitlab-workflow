package com.mitlab.workflow;

import java.util.List;
import java.util.Map;

public interface UserGroupLoader {
	List<UserGroup> loadUsers(Map<String, Object> inputs, Map<String, Object> args, String userGroup);
}
