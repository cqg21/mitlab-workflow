package com.mitlab.workflow;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public interface Step extends Serializable {
	public Long getId();
	
	public Long getPrevId();

	public String getStepId();

	public String getActionId();

	public String getUserGroup();

	public String getCaller();

	public String getStepName();

	public Date getStartDate();

	public Date getDueDate();

	public Date getFinishDate();

	public Long getWorkflowId();

	public String getStatus();

	public Map<String, String> getMeta();

	public String getWorkflowName();

	public Map<String, Object> getArgs();
	
	public Map<String, Object> getProperties();
}
