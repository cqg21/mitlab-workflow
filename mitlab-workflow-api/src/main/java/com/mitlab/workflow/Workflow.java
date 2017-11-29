package com.mitlab.workflow;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Workflow extends Serializable {
	
	List<Action> getAvailableActions(long workflowId, Map<String, Object> inputs, WorkflowUser caller);
	
	List<Action> getAvailableActionsWithSubflow(long workflowId, Map<String, Object> inputs, WorkflowUser caller);
	
	List<Action> getAvailableActionsWithSubflow(long workflowId, Long stepId, Map<String, Object> inputs, WorkflowUser caller);

	List<Step> getCurrentSteps(long workflowId);
	
	List<Step> getCurrentStepsWithSubflow(long workflowId);

	List<Step> getHistorySteps(long workflowId);
	
	List<Step> getHistoryStepsWithSubflow(long workflowId);

	ActionResult startWorkflow(String workflowName, Map<String, Object> inputs, WorkflowUser caller);

	ActionResult doAction(long workflowId, Long stepId, String actionId, Map<String, Object> inputs, WorkflowUser caller);

	Set<Long> getToDoWorkflows(String workflowName, WorkflowUser caller);

	Set<Long> getDoneWorkflows(String workflowName, WorkflowUser caller);
	
	Map<String, Boolean> editable(long workflowId, long stepId, Map<String, Object> inputs, WorkflowUser caller);
	
	Map<String, Boolean> editable(long workflowId, long stepId, Map<String, Object> inputs, boolean considerAction, WorkflowUser caller);
	
	public static final String ARG_WORKFLOW_DS = "__arg_workflow_datasource";
	public static final String ARG_WORKFLOW_ID = "__arg_workflow_id";
	public static final String ARG_WORKFLOW = "__arg_workflow";
	public static final String ARG_STEP_ID = "__arg_current_step_id";
	public static final String ARG_ACTION_CALLER = "__arg_action_caller";
	
	public static final String ARG_RESULT_DESCRIPTOR = "ARG_RESULT_DESCRIPTOR";
	public static final String ARG_LOADED_USER_GROUP = "ARG_LOADED_USER_GROUP";
	
	public static final String INITIALIZE = "initialize";
	public static final String MAIN_FLOW_ID = "main_flow_id";
	public static final String MAIN_FLOW_JOIN_STEP_ID = "main_flow_join_step_id";
}
