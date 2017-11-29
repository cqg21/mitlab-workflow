package com.mitlab.workflow;

import java.util.Set;

import com.mitlab.workflow.WorkflowUser;

public class SimpleWorkflowUser implements WorkflowUser {
	private static final long serialVersionUID = 1L;
	private final String username;
	private final Set<String> authorities;
	
	public SimpleWorkflowUser(String username, Set<String> authorities) {
		this.username = username;
		this.authorities = authorities;
	}
	
	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean hasPermission(String permission) {
		return authorities.contains(permission);
	}

}
