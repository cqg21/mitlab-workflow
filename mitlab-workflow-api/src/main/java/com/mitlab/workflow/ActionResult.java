package com.mitlab.workflow;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class ActionResult implements Serializable {
	private static final long serialVersionUID = 1L;
	private boolean result;
	private Long workflowId;
	private FlowStatus flowStatus;
	private Long currentStepId;
	private String currentStepStatus;
	private String currentStepStepId;
	private Long newStepId;
	private String newStepStatus;
	private String newStepStepId;
	private String desc;
	public List<Long> subflowIds;
	
	private FlowPhase flowPhase;
	
	@SuppressWarnings("unchecked")
	public List<Long> getSubflowIds() {
		if (subflowIds == null) {
			subflowIds = Collections.EMPTY_LIST;
		}
		return subflowIds;
	}

	public void setSubflowIds(List<Long> subflowIds) {
		this.subflowIds = subflowIds;
	}

	public boolean isSuccess() {
		return result;
	}

	protected void setResult(boolean result) {
		this.result = result;
	}

	public Long getWorkflowId() {
		return workflowId;
	}

	protected void setWorkflowId(Long workflowId) {
		this.workflowId = workflowId;
	}

	public FlowStatus getFlowStatus() {
		return flowStatus;
	}

	protected void setFlowStatus(FlowStatus workflowStatus) {
		this.flowStatus = workflowStatus;
	}

	public Long getCurrentStepId() {
		return currentStepId;
	}

	protected void setCurrentStepId(Long currentStepId) {
		this.currentStepId = currentStepId;
	}

	public String getCurrentStepStatus() {
		return currentStepStatus;
	}

	protected void setCurrentStepStatus(String currentStepStatus) {
		this.currentStepStatus = currentStepStatus;
	}

	public String getCurrentStepStepId() {
		return currentStepStepId;
	}

	protected void setCurrentStepStepId(String currentStepStepId) {
		this.currentStepStepId = currentStepStepId;
	}

	public Long getNewStepId() {
		return newStepId;
	}

	protected void setNewStepId(Long newStepId) {
		this.newStepId = newStepId;
	}

	public String getNewStepStatus() {
		return newStepStatus;
	}

	public void setNewStepStatus(String newStepStatus) {
		this.newStepStatus = newStepStatus;
	}

	public String getNewStepStepId() {
		return newStepStepId;
	}

	public void setNewStepStepId(String newStepStepId) {
		this.newStepStepId = newStepStepId;
	}

	public FlowPhase getFlowPhase() {
		return flowPhase;
	}

	public void setFlowPhase(FlowPhase flowPhase) {
		this.flowPhase = flowPhase;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	@Override
	public String toString() {
		return "ActionResult [result=" + result + ", workflowId=" + workflowId + ", flowStatus=" + flowStatus
				+ ", currentStepId=" + currentStepId + ", currentStepStatus=" + currentStepStatus
				+ ", currentStepStepId=" + currentStepStepId + ", newStepId=" + newStepId + ", newStepStatus="
				+ newStepStatus + ", newStepStepId=" + newStepStepId + ", desc=" + desc + ", flowPhase=" + flowPhase
				+ "]";
	}

}
