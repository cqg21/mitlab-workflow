package com.mitlab.workflow;

import java.io.Serializable;

public interface WorkflowUser extends Serializable {
	public String getUsername();
	
	public boolean hasAuthority(String permission);
}
