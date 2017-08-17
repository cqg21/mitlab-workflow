package com.mitlab.workflow;

import java.io.Serializable;

public class UserGroup implements Serializable {
	private static final long serialVersionUID = 1L;
	private String group;
	private String user;

	public UserGroup() {
		
	}
	
	public UserGroup(String user, String group) {
		this.user = user;
		this.group = group;
	}
	
	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	@Override
	public String toString() {
		return "UserGroup [group=" + group + ", user=" + user + "]";
	}
	
}
