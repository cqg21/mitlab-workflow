package com.mitlab.workflow;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public interface Step extends Serializable {
	Long getId();
	
	Long getPrevId();

	String getStepId();

	String getActionId();

	String getUserGroup();

	String getCaller();

	String getStepName();

	Date getStartDate();

	Date getDueDate();

	Date getFinishDate();

	Long getWorkflowId();

	String getStatus();

	Map<String, String> getMeta();

	String getWorkflowName();

	Map<String, Object> getArgs();
	
	Map<String, Object> getProperties();
}
