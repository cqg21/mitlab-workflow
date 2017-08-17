package com.mitlab.workflow;

import java.io.Serializable;
import java.util.Map;

public interface Action extends Serializable {
	public Long getStepId();
	
	public String getActionId();
	
	public String getActionName();
	
	public Map<String, String> getMeta();
	
	public String getStepStepId();
	
	public Long getWorkflowId();
	
	public String getWorkflowName();
}
