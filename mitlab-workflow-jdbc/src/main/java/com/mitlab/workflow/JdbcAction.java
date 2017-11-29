package com.mitlab.workflow;

import java.util.Map;

public class JdbcAction implements Action {
	private static final long serialVersionUID = 1L;
	private Long stepId;
	private String actionId;
	private String actionName;
	private Map<String, String> meta;

	private String stepStepId;
	private Long workflowId;
	private String workflowName;

	public Long getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(Long workflowId) {
		this.workflowId = workflowId;
	}

	public String getWorkflowName() {
		return workflowName;
	}

	public void setWorkflowName(String workflowName) {
		this.workflowName = workflowName;
	}

	public String getActionId() {
		return actionId;
	}

	public void setActionId(String actionId) {
		this.actionId = actionId;
	}

	public String getStepStepId() {
		return stepStepId;
	}

	public void setStepStepId(String stepStepId) {
		this.stepStepId = stepStepId;
	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public Map<String, String> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, String> meta) {
		this.meta = meta;
	}

	public Long getStepId() {
		return stepId;
	}

	public void setStepId(Long stepId) {
		this.stepId = stepId;
	}

}
