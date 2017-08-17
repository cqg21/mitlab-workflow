package com.mitlab.workflow;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Workflow extends Serializable {
	
	public List<Action> getAvailableActions(long workflowId, Map<String, Object> inputs, WorkflowUser caller);
	
	public List<Action> getAvailableActionsWithSubflow(long workflowId, Map<String, Object> inputs, WorkflowUser caller);
	
	public List<Action> getAvailableActionsWithSubflow(long workflowId, Long stepId, Map<String, Object> inputs, WorkflowUser caller);

	public List<Step> getCurrentSteps(long workflowId);
	
	public List<Step> getCurrentStepsWithSubflow(long workflowId);

	public List<Step> getHistorySteps(long workflowId);
	
	public List<Step> getHistoryStepsWithSubflow(long workflowId);

	public ActionResult startWorkflow(String workflowName, Map<String, Object> inputs, WorkflowUser caller);

	/**
	 * @see Workflow#doAction(long, Long, String, Map, WorkflowUser)
	 */
	@Deprecated
	public ActionResult doAction(long workflowId, String actionId, Map<String, Object> inputs, WorkflowUser caller);
	
	public ActionResult doAction(long workflowId, Long stepId, String actionId, Map<String, Object> inputs, WorkflowUser caller);

	/**
	 * 查询待办流程
	 */
	public Set<Long> getToDoWorkflows(String workflowName, WorkflowUser caller);

	/**
	 * 查询已办流程
	 */
	public Set<Long> getDoneWorkflows(String workflowName, WorkflowUser caller);
	
	/**
	 * @see Workflow#editable(long, long, Map, WorkflowUser)
	 */
	@Deprecated
	public Map<String, Boolean> editable(long workflowId, Map<String, Object> inputs, WorkflowUser caller);
	
	public Map<String, Boolean> editable(long workflowId, long stepId, Map<String, Object> inputs, WorkflowUser caller);
	
	/**
	 *	@see Workflow#editable(long, long, Map, boolean, WorkflowUser) 
	 */
	@Deprecated
	public Map<String, Boolean> editable(long workflowId, Map<String, Object> inputs, boolean considerAction, WorkflowUser caller);
	
	public Map<String, Boolean> editable(long workflowId, long stepId, Map<String, Object> inputs, boolean considerAction, WorkflowUser caller);
	
	public String horizontalFlowGraph(String workflowName);
	
	public String horizontalFlowGraph(Long workflowId);
	
	public String verticalFlowGraph(Long workflowId);
	
	public String verticalFlowGraph(String workflowName);
	
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
