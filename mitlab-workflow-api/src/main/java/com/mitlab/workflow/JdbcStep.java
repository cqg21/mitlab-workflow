package com.mitlab.workflow;

import java.util.Date;
import java.util.Map;

public class JdbcStep implements Step {
	private static final long serialVersionUID = 1L;
	private Long id;
	private Long prevId;
	private String stepId;
	private String stepName;
	private String userGroup;
	private String caller;
	private Date startDate;
	private String status;
	private String actionId;
	private Date dueDate;
	private Date finishDate;
	
	private Long workflowId;
	private String workflowName;
	
	private Map<String, String> meta;
	private Map<String, Object> args;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getStepId() {
		return stepId;
	}

	public void setStepId(String stepId) {
		this.stepId = stepId;
	}
	
	public String getWorkflowName() {
		return workflowName;
	}

	public void setWorkflowName(String workflowName) {
		this.workflowName = workflowName;
	}

	public String getStepName() {
		return stepName;
	}

	public void setStepName(String stepName) {
		this.stepName = stepName;
	}

	public String getUserGroup() {
		return userGroup;
	}

	public void setUserGroup(String userGroup) {
		this.userGroup = userGroup;
	}

	public String getCaller() {
		return caller;
	}

	public void setCaller(String caller) {
		this.caller = caller;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Long getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(Long workflowId) {
		this.workflowId = workflowId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getActionId() {
		return actionId;
	}

	public void setActionId(String actionId) {
		this.actionId = actionId;
	}

	public Date getDueDate() {
		return dueDate;
	}

	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}

	public Date getFinishDate() {
		return finishDate;
	}

	public void setFinishDate(Date finishDate) {
		this.finishDate = finishDate;
	}

	public Map<String, String> getMeta() {
		return meta;
	}

	public void setMeta(Map<String, String> meta) {
		this.meta = meta;
	}
	
	public Map<String, Object> getArgs() {
		return args;
	}
	
	

	@Override
	public Map<String, Object> getProperties() {
		return this.getArgs();
	}

	public void setArgs(Map<String, Object> args) {
		this.args = args;
	}
	
	public Long getPrevId() {
		return prevId;
	}

	public void setPrevId(Long prevId) {
		this.prevId = prevId;
	}

	@Override
	public String toString() {
		return "JdbcStep [id=" + id + ", prevId=" + prevId + ", stepId=" + stepId + ", stepName=" + stepName
				+ ", userGroup=" + userGroup + ", caller=" + caller + ", startDate=" + startDate + ", status=" + status
				+ ", actionId=" + actionId + ", dueDate=" + dueDate + ", finishDate=" + finishDate + ", workflowId="
				+ workflowId + ", workflowName=" + workflowName + ", meta=" + meta + ", args=" + args + "]";
	}
	
}
