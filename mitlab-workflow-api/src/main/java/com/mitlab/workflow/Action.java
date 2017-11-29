package com.mitlab.workflow;

import java.io.Serializable;
import java.util.Map;

public interface Action extends Serializable {
	Long getStepId();
	
	String getActionId();
	
	String getActionName();
	
	Map<String, String> getMeta();
	
	String getStepStepId();
	
	Long getWorkflowId();
	
	String getWorkflowName();
}
