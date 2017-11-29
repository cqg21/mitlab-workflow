package com.mitlab.workflow;

import java.io.Serializable;

public interface WorkflowUser extends Serializable {
	String getUsername();
	
	boolean hasAuthority(String permission);
}
