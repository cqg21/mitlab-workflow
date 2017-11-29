package com.mitlab.workflow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianFactory;
import com.caucho.hessian.io.SerializerFactory;
import com.mitlab.workflow.descriptor.ActionDescriptor;
import com.mitlab.workflow.descriptor.ActionsDescriptor;
import com.mitlab.workflow.descriptor.ArgDescriptor;
import com.mitlab.workflow.descriptor.ConditionDescriptor;
import com.mitlab.workflow.descriptor.ConditionsDescriptor;
import com.mitlab.workflow.descriptor.FunctionDescriptor;
import com.mitlab.workflow.descriptor.FunctionsDescriptor;
import com.mitlab.workflow.descriptor.MetaDescriptor;
import com.mitlab.workflow.descriptor.ResultDescriptor;
import com.mitlab.workflow.descriptor.ResultsDescriptor;
import com.mitlab.workflow.descriptor.StepDescriptor;
import com.mitlab.workflow.descriptor.WorkflowDescriptor;

public class JdbcWorkflow implements Workflow {
	private static final long serialVersionUID = 1L;
	private final Log logger = LogFactory.getLog(this.getClass());
	private final UserGroupLoader userGroupLoader;
	private final DataSource dataSource;
	private static final ThreadLocal<HessianFactory> hessianFactory = new ThreadLocal<HessianFactory>() {
		@Override
		protected HessianFactory initialValue() {
			return new HessianFactory();
		}
		
	};
	
	public JdbcWorkflow(UserGroupLoader userGroupLoader, DataSource dataSource) {
		this.userGroupLoader = userGroupLoader;
		this.dataSource = dataSource;
	}
	
	private interface StepLookup {
		public void lookup(Step step, StepDescriptor stepDescriptor);
	}

	@Override
	public List<Action> getAvailableActions(final long workflowId, final Map<String, Object	> inputs, final WorkflowUser caller) {
		List<Action> actions = new ArrayList<Action>(0);
		Connection conn = null;
		try {
			conn = newConnection();
			actions = this.getAvailableActions(workflowId, null, inputs, caller, conn, false);
		} catch (MitlabWorkflowException e) {
			throw e;
		} catch (Exception e) {
			throw new MitlabWorkflowException("getCurrentSteps Error", e);
		} finally {
			WorkflowUtil.close(conn);
		}
		return actions;
	}
	
	@Override
	public List<Action> getAvailableActionsWithSubflow(long workflowId, Map<String, Object> inputs, WorkflowUser caller) {
		return this.getAvailableActionsWithSubflow(workflowId, null, inputs, caller);
	}
	
	
	@Override
	public List<Action> getAvailableActionsWithSubflow(long workflowId, Long stepId, Map<String, Object> inputs, WorkflowUser caller) {
		List<Action> actions = new ArrayList<Action>(0);
		Connection conn = null;
		try {
			conn = newConnection();
			actions = this.getAvailableActions(workflowId, stepId, inputs, caller, conn, true);
		} catch (MitlabWorkflowException e) {
			throw e;
		} catch (Exception e) {
			throw new MitlabWorkflowException("getAvailableActionsWithSubflow Error", e);
		} finally {
			WorkflowUtil.close(conn);
		}
		return actions;
	}

	public List<Action> getAvailableActions(final long workflowId, final Long stepId, final Map<String, Object	> inputs, final WorkflowUser caller, final Connection conn, final boolean withSubflow) {
		final List<Action> actions = new ArrayList<Action>();
		getCurrentSteps(conn, workflowId, new StepLookup() {
			@Override
			public void lookup(Step step, StepDescriptor stepDescriptor) {
				if (stepId != null) {
					if (stepId.longValue() != step.getId().longValue()) {
						return;
					}
				}
				ActionsDescriptor asd = stepDescriptor.getActions();
				if (asd == null) {
					return;
				}
				List<ActionDescriptor> ads = asd.getAction();
				Map<String, Object> args = new HashMap<String, Object>();
				args.put(Workflow.ARG_WORKFLOW_ID, workflowId);
				args.put(Workflow.ARG_STEP_ID, step.getId());
				args.put(Workflow.ARG_ACTION_CALLER, caller);
				args.put(Workflow.ARG_WORKFLOW_DS, getDataSource());
				for (ActionDescriptor ad : ads) {
					ConditionsDescriptor csd = ad.getConditions();
					if (!passCondition(csd, inputs,args, caller)) {
						continue;
					}
					actions.add(newJdbcAction(step, ad));
				}
			}
		});
		if (withSubflow && !(stepId != null && !actions.isEmpty() /*针对某个stepId已找到动说，不必再往下找*/)) {
			List<Long> subflowIds = getSubflowIds(conn, workflowId);
			for (Long subflowId : subflowIds) {
				actions.addAll(getAvailableActions(subflowId, stepId, inputs, caller, conn, withSubflow));
			}
		}
		return actions;
	}
	
	private JdbcAction newJdbcAction(Step step, ActionDescriptor ad) {
		List<MetaDescriptor> metaList = ad.getMeta();
		JdbcAction action = new JdbcAction();
		action.setStepId(step.getId());
		action.setStepStepId(step.getStepId());
		action.setWorkflowId(step.getWorkflowId());
		action.setWorkflowName(step.getWorkflowName());
		action.setActionId(ad.getId());
		action.setActionName(ad.getName());
		Map<String, String> meta = new HashMap<String, String>();
		for (MetaDescriptor md : metaList) {
			meta.put(md.getKey(), md.getValue());
		}
		action.setMeta(meta);
		return action;
	}
	
	private boolean passCondition(ConditionsDescriptor conditionsDescriptor, final Map<String, Object> inputs, Map<String, Object> args, final WorkflowUser caller) {
		if (conditionsDescriptor == null) {
			return true;
		}
		boolean passCondition = false;
		List<ConditionDescriptor> conditionDescriptors = conditionsDescriptor.getCondition();
		if ("OR".equals(conditionsDescriptor.getType())) {
			for (ConditionDescriptor cd : conditionDescriptors) {
				List<ArgDescriptor> argList = cd.getArg();
				for (ArgDescriptor arg : argList) {
					args.put(arg.getName(), arg.getValue());
				}
				if ("spring".equals(cd.getType())) {
					Condition condition = WorkflowResolver.getInstance().getCondition((String) args.get("bean.id"));
					passCondition = passCondition || condition.passesCondition(inputs, args, caller);
					if (passCondition) {
						break;
					}
				} else {
					throw new MitlabWorkflowException("Unsupported Condition type:" + conditionsDescriptor.getType());
				}
			}
			ConditionsDescriptor childConditions = conditionsDescriptor.getConditions();
			if (!passCondition && childConditions != null && (!childConditions.getCondition().isEmpty() || (childConditions.getConditions() != null && !childConditions.getConditions().getCondition().isEmpty()))) {
				passCondition = passCondition(childConditions, inputs, args, caller);
			}
		} else/*if ("AND".equals(conditionsDescriptor.getType()))*/ {
			boolean localCondition = true;
			for (ConditionDescriptor conditionDescriptor : conditionDescriptors) {
				List<ArgDescriptor> argList = conditionDescriptor.getArg();
				for (ArgDescriptor arg : argList) {
					args.put(arg.getName(), arg.getValue());
				}
				if ("spring".equals(conditionDescriptor.getType())) {
					Condition condition = WorkflowResolver.getInstance().getCondition((String) args.get("bean.id"));
					localCondition = condition.passesCondition(inputs, args, caller);
					if (!localCondition) {
						break;
					}
				} else {
					throw new MitlabWorkflowException("Unsupported Condition type:" + conditionsDescriptor.getType());
				}
			}
			ConditionsDescriptor childConditions = conditionsDescriptor.getConditions();
			if (localCondition && childConditions != null && (!childConditions.getCondition().isEmpty() || (childConditions.getConditions() != null && !childConditions.getConditions().getCondition().isEmpty()))) {
				localCondition = passCondition(childConditions, inputs, args, caller);
			}
			passCondition = localCondition;
		}
		return passCondition;
	}

	private static final String GET_CURRENT_STEPS_SQL = "select s.id, s.step_id, s.step_name, s.user_group, s.caller, s.start_date, s.due_date, s.finish_date, s.status, s.workflow_id, s.action_id,w.workflow_name, prev_id from t_current_step s, t_workflow w where s.workflow_id = w.workflow_id and w.workflow_id = ?";
	@SuppressWarnings("unchecked")
	public static List<Step> getCurrentSteps(Connection conn, long workflowId, StepLookup stepLookup) {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<? extends Step> steps = new ArrayList<Step>(0);
		try {
			steps = getSteps(workflowId, conn, GET_CURRENT_STEPS_SQL, stepLookup);
			stmt = conn.prepareStatement(GET_STEP_ARGS_SQL);
			for (Step step : steps) {
				if (!(step instanceof JdbcStep)) {
					continue;
				}
				stmt.setLong(1, step.getWorkflowId());
				stmt.setLong(2, step.getId());
				stmt.setInt(3, 0);
				rs = stmt.executeQuery();
				loadStepArgs(rs, (JdbcStep) step);
				WorkflowUtil.close(rs);
			}
			WorkflowUtil.close(stmt);
		} catch (MitlabWorkflowException e) {
			throw e;
		} catch (Exception e) {
			throw new MitlabWorkflowException("getHistorySteps Error", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
		}
		return (List<Step>) steps;
	}

	private static void loadStepArgs(ResultSet rs, JdbcStep step) throws SQLException {
		if (rs.next()) {
            ByteArrayInputStream bins = new ByteArrayInputStream(rs.getBytes(1));
            Hessian2Input hins = hessianFactory.get().createHessian2Input(bins);
            hins.setCloseStreamOnClose(true);
            hins.setSerializerFactory(new SerializerFactory());
            try {
                Map<String, Object> args = (Map<String, Object>) hins.readObject();
                step.setArgs(args);
                hins.close();
            } catch (IOException e) {
                throw new MitlabWorkflowException("close houts error", e);
            }
        }
	}

	@Override
	public List<Step> getCurrentSteps(long workflowId) {
		Connection conn = null;
		List<Step> steps = new ArrayList<Step>(0);
		try {
			conn = newConnection();
			steps = getCurrentSteps(conn, workflowId, null);
		} catch (MitlabWorkflowException e) {
			throw e;
		} catch (Exception e) {
			throw new MitlabWorkflowException("getCurrentSteps Error", e);
		} finally {
			WorkflowUtil.close(conn);
		}
		return (List<Step>) steps;
	}

	@Override
	public List<Step> getHistorySteps(long workflowId) {
		Connection conn = null;
		List<Step> steps;
		try {
			conn = newConnection();
			steps = getHistorySteps(workflowId, conn);
		} catch (MitlabWorkflowException e) {
			throw e;
		} finally {
			WorkflowUtil.close(conn);
		}
		return steps;
	}
	
	@Override
	public List<Step> getHistoryStepsWithSubflow(long workflowId) {
		Connection conn = null;
		List<Step> steps;
		try {
			conn = newConnection();
			steps = getHistoryStepsWithSubflow(workflowId, conn);
		} catch (MitlabWorkflowException e) {
			throw e;
		} finally {
			WorkflowUtil.close(conn);
		}
		return steps;
	}
	
	private List<Step> getHistoryStepsWithSubflow(long workflowId, Connection conn) {
		List<Step> steps;
		try {
			steps = getHistorySteps(workflowId, conn);
			List<Long> subflowIds = getSubflowIds(conn, workflowId);
			for (Long subflowId : subflowIds) {
				steps.addAll(getHistoryStepsWithSubflow(subflowId, conn));
			}
		} catch (MitlabWorkflowException e) {
			throw e;
		} 
		return steps;
	}

	private static final String GET_HISTORY_STEPS_SQL = "select s.id, s.step_id, s.step_name, s.user_group, s.caller, s.start_date, s.due_date, s.finish_date, s.status, s.workflow_id, s.action_id,w.workflow_name, s.prev_id from t_history_step s, t_workflow w where s.workflow_id = w.workflow_id and w.workflow_id = ?";
	private static final String GET_STEP_ARGS_SQL = "select step_args from t_step_args where workflow_id = ? and step_id = ? and args_type = ?";
	public List<Step> getHistorySteps(long workflowId, Connection conn) {
		PreparedStatement stmt = null;
		ResultSet rs = null;
		List<Step> steps = new LinkedList<Step>();
		try {
			List<Step> stepList = getSteps(workflowId,conn, GET_HISTORY_STEPS_SQL, null);
			stmt = conn.prepareStatement(GET_STEP_ARGS_SQL);
			for (Step step : stepList) {
				if (!(step instanceof JdbcStep)) {
					continue;
				}
				stmt.setLong(1, step.getWorkflowId());
				stmt.setLong(2, step.getId());
				stmt.setInt(3, 1);
				rs = stmt.executeQuery();
				loadStepArgs(rs, (JdbcStep) step);
				WorkflowUtil.close(rs);
			}
			WorkflowUtil.close(stmt);
			Map<Long, Step> msteps = new HashMap<Long, Step>(stepList.size());
			for (Step step : stepList) {
				Long prevStepId = step.getPrevId();
				if (prevStepId == null) {
					prevStepId = Long.MIN_VALUE;
					if (msteps.containsKey(prevStepId)) {
						throw new MitlabWorkflowException("unexpired prevStepId, more than one Long.MIN_VALUE as prevStepId!");
					}
				}
				msteps.put(prevStepId, step);
			}
			Long prevStepId = Long.MIN_VALUE;
			while (!msteps.isEmpty()) {
				if (!msteps.containsKey(prevStepId)) {
					break;
				}
				Step step = msteps.remove(prevStepId);
				/*steps.add(0, step);*/
				steps.add(step);
				prevStepId = step.getId();
			}
		} catch (MitlabWorkflowException e) {
			throw e;
		} catch (Exception e) {
			throw new MitlabWorkflowException("getHistorySteps Error", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
		}
		return steps;
	}

	private static List<Step> getSteps(long workflowId, Connection conn, String sql, StepLookup stepLookup) {
		List<Step> steps = new ArrayList<Step>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(sql);
			stmt.setLong(1, workflowId);
			rs = stmt.executeQuery();
			while (rs.next()) {
				steps.add(newJdbcStep(stepLookup, rs));
			}
		} catch (SQLException e) {
			throw new MitlabWorkflowException("查询工作流步骤失败", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
		}
		return steps;
	}

	private static JdbcStep newJdbcStep(StepLookup stepLookup, ResultSet rs) throws SQLException {
		JdbcStep step = new JdbcStep();
		step.setId(rs.getLong(1));
		step.setStepId(rs.getString(2));
		step.setStepName(rs.getString(3));
		step.setUserGroup(rs.getString(4));
		step.setCaller(rs.getString(5));
		Timestamp timestamp = rs.getTimestamp(6);
		step.setStartDate(timestamp == null ? null : new Date(timestamp.getTime()));
		timestamp = rs.getTimestamp(7);
		step.setDueDate(timestamp == null ? null : new Date(timestamp.getTime()));
		timestamp = rs.getTimestamp(8);
		step.setFinishDate(timestamp == null ? null : new Date(timestamp.getTime()));
		step.setStatus(rs.getString(9));
		step.setWorkflowId(rs.getLong(10));
		step.setActionId(rs.getString(11));
		step.setWorkflowName(rs.getString(12));
		String prevId = rs.getString(13);
		if (prevId != null) {
			step.setPrevId(Long.parseLong(prevId));
		}
		StepDescriptor stepDecriptor = WorkflowResolver.getInstance().getStepDecriptor(step.getWorkflowName(), step.getStepId());
		List<MetaDescriptor> metaList = stepDecriptor.getMeta();
		Map<String, String> meta = new HashMap<String, String>(metaList.size());
		for (MetaDescriptor md : metaList) {
			meta.put(md.getKey(), md.getValue());
		}
		step.setMeta(meta);
		if (stepLookup != null) {
			stepLookup.lookup(step, stepDecriptor);
		}
		return step;
	}
	
	private Connection newConnection() {
		Connection conn = null;
		try {
			conn = getDataSource().getConnection();
		} catch (Throwable e) {
			throw new MitlabWorkflowException("取得数据库连接出错", e);
		}
		return conn;
	}
	
	private DataSource getDataSource() {
		return this.dataSource;
	}

	private static final String SELECT_INSERT_ID = "select last_insert_id()";
	private static final String INSERT_WORKFLOW_SQL = "insert into t_workflow(workflow_name, workflow_status, workflow_phase, main_flow_id, main_flow_join_step_id) values(?, ?, ?, ?, ?)";
	@Override
	public ActionResult startWorkflow(String workflowName, Map<String, Object> inputs, WorkflowUser caller) {
		ActionResult actionResult = new ActionResult();
		Connection conn = null;
		Boolean autoCommit = null;
		try {
			conn = newConnection();
			autoCommit = WorkflowUtil.getAutoCommit(conn);
			WorkflowUtil.setAutoCommit(conn, false);
			actionResult = startWorkflow(conn, workflowName, inputs, caller);
		} catch (Exception e) {
			actionResult.setResult(false);
			actionResult.setDesc("创建工作流步骤失败:" + e.getMessage());
			logger.error(actionResult.getDesc(), e);
			WorkflowUtil.rollback(conn);
		} finally {
			WorkflowUtil.setAutoCommit(conn, autoCommit);
			WorkflowUtil.close(conn);
		}
		return actionResult;
	}
	
	public ActionResult startWorkflow(Connection conn, String workflowName, Map<String, Object> inputs, WorkflowUser caller) throws SQLException {
		ActionResult actionResult = new ActionResult();
		if (inputs == null) {
			inputs = new HashMap<String, Object>();
		}
		inputs.put(Workflow.INITIALIZE, true);
		WorkflowDescriptor workflowDescriptor = WorkflowResolver.getInstance().getWorkflowDescriptor(workflowName);
		StepDescriptor startStepDescriptor = workflowDescriptor.getStartStep();
		List<ActionDescriptor> actionDescriptorList = startStepDescriptor.getActions().getAction();
		if (actionDescriptorList.size() != 1) {
			// 错误处理
		}
		ActionDescriptor initialAction = actionDescriptorList.get(0);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(INSERT_WORKFLOW_SQL);
			stmt.setString(1, workflowName);
			actionResult.setFlowStatus(FlowStatus.CREATED);
			actionResult.setFlowPhase(com.mitlab.workflow.FlowPhase.ON_START);
			stmt.setString(2, actionResult.getFlowStatus().toString());
			stmt.setString(3, actionResult.getFlowPhase().toString());
			stmt.setObject(4, inputs.get(Workflow.MAIN_FLOW_ID));
			stmt.setObject(5, inputs.get(Workflow.MAIN_FLOW_JOIN_STEP_ID));
			stmt.executeUpdate();
			WorkflowUtil.close(stmt);
			stmt = conn.prepareStatement(SELECT_INSERT_ID);
			rs = stmt.executeQuery();
			rs.next();
			long workflowId = rs.getLong(1);
			WorkflowUtil.close(rs);
			JdbcAction doingAction = new JdbcAction();
			doingAction.setActionId(initialAction.getId());
			doingAction.setActionName(initialAction.getName());
			doingAction.setStepStepId(startStepDescriptor.getId());
			doingAction.setWorkflowId(workflowId);
			doingAction.setWorkflowName(workflowName);
			doingAction.setStepId(-System.nanoTime());
			doAction(conn, doingAction.getWorkflowId(), doingAction, inputs, startStepDescriptor, caller, actionResult);
			WorkflowUtil.commit(conn);
			actionResult.setResult(true);
			actionResult.setDesc("success");
			actionResult.setWorkflowId(workflowId);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
		}
		return actionResult;
	}
	
	private String getWorkflowNameById(Connection conn, Long workflowId) {
		String workflowName = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement("select workflow_name from t_workflow where workflow_id = ?");
			stmt.setLong(1, workflowId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				workflowName = rs.getString(1);
			}
		} catch (SQLException e) {
			throw new MitlabWorkflowException("workflow[" + workflowId + "] is not exists!");
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
		}
		return workflowName;
	}
	
	private String getWorkflowStatusById(Connection conn, Long workflowId) {
		String workflowStatus = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement("select workflow_status from t_workflow where workflow_id = ?");
			stmt.setLong(1, workflowId);
			rs = stmt.executeQuery();
			if (rs.next()) {
				workflowStatus = rs.getString(1);
			}
		} catch (SQLException e) {
			throw new MitlabWorkflowException("workflow[" + workflowId + "] is not exists!");
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
		}
		return workflowStatus;
	}
	
	private static final String INSERT_CURRENT_STEP_SQL = "insert t_current_step(step_id, step_name, user_group, start_date, status, workflow_id, prev_id) values(?, ?, ?, ?, ?, ?, ?)";
	private static final String INSERT_HISTORY_STEP_SQL = "insert t_history_step(id, step_id, step_name, user_group, caller, start_date, due_date, finish_date, status, workflow_id, action_id, prev_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String GET_CURRENT_STEPS_BY_ID_SQL = "select s.id, s.step_id, s.step_name, s.user_group, s.caller, s.start_date, s.due_date, s.finish_date, s.status, s.workflow_id, s.action_id,w.workflow_name, s.prev_id from t_current_step s, t_workflow w where s.workflow_id = w.workflow_id and w.workflow_id = ? and s.step_id = ?";
	private static final String DELETE_CURRENT_STEP_BY_ID_SQL = "delete from t_current_step where workflow_id = ? and id = ?";
	private static final String UPDATE_WORKFLOW_STATE_SQL = "update t_workflow set workflow_status = ?, workflow_phase = ? where workflow_id = ?";
	// create table t_step_args(workflow_id bigint not null, step_id bigint not null, step_args longtext, args_type int not null, primary key(workflow_id,step_id,args_type)); // 0:current_step;1:history_step;
	private static final String INSERT_STEP_ARGS_SQL = "insert into t_step_args(workflow_id,step_id,step_args, args_type) values(?, ?, ?, ?)";
	private void doAction(Connection conn, Long workflowId, Action doingAction, Map<String, Object> inputs, StepDescriptor currentStepDescriptor, WorkflowUser caller, ActionResult actionResult) {
		actionResult.setWorkflowId(workflowId);
		actionResult.setCurrentStepId(doingAction.getStepId());
		actionResult.setCurrentStepStepId(doingAction.getStepStepId());
		Map<String, Object> args = new HashMap<String, Object>();
		args.put(Workflow.ARG_WORKFLOW, this);
		args.put(Workflow.ARG_WORKFLOW_ID, workflowId);
		args.put(Workflow.ARG_STEP_ID, doingAction.getStepId());
		args.put(Workflow.ARG_ACTION_CALLER, caller);
		args.put(Workflow.ARG_WORKFLOW_DS, getDataSource());
		ActionDescriptor doingActionDescriptor = null;
		List<ActionDescriptor> actionDescriptorList = currentStepDescriptor.getActions().getAction();
		for (ActionDescriptor actionDescriptor : actionDescriptorList) {
			if (!passCondition(actionDescriptor.getConditions(), inputs, args, caller)) {
				continue;
			}
			if (actionDescriptor.getId().equals(doingAction.getActionId())) {
				doingActionDescriptor = actionDescriptor;
				break;
			}
		}
		if (doingActionDescriptor == null) {
			actionResult.setDesc("Action[" + doingAction.getActionId() + "] is invalid for workflow[" + workflowId + "]");
			throw new MitlabWorkflowException(actionResult.getDesc());
		}
		ResultsDescriptor resultsDescriptor = doingActionDescriptor.getResults();
		List<ResultDescriptor> resultDescriptorList = resultsDescriptor.getResult();
		ResultDescriptor nextResultDescriptor = null;
		for (ResultDescriptor resultDescriptor : resultDescriptorList) {
			if (this.passCondition(resultDescriptor.getConditions(), inputs, args, caller)) {
				nextResultDescriptor = resultDescriptor;
				break;
			}
		}
		if (nextResultDescriptor == null) {
			actionResult.setDesc("No valid Result for workflow[" + workflowId + "] action[" + doingAction.getActionId() + "]");
			throw new MitlabWorkflowException(actionResult.getDesc());
		}
		args.put(Workflow.ARG_RESULT_DESCRIPTOR, nextResultDescriptor);
		final String workflowName = this.getWorkflowNameById(conn, workflowId);
		final StepDescriptor newStepDescriptor = WorkflowResolver.getInstance().getStepDecriptor(workflowName, nextResultDescriptor.getStep());
		Map<String, Object> propertyStore = new HashMap<String, Object>();
		executeFunctions(inputs, args, propertyStore, newStepDescriptor.getPreFunctions(),false);
		executeFunctions(inputs, args, propertyStore, doingActionDescriptor.getPreFunctions(),false);
		executeFunctions(inputs, args, propertyStore, nextResultDescriptor.getPreFunctions(),false);
		PreparedStatement stmt = null;
		ResultSet rs = null;
		final WorkflowDescriptor workflowDescriptor = WorkflowResolver.getInstance().getWorkflowDescriptor(workflowName);
		final boolean newStepIsStopStep = newStepDescriptor.equals(workflowDescriptor.getStopStep());
		try {
			JdbcStep historyStep = null;
			if ((Boolean) inputs.get(Workflow.INITIALIZE)) {
				historyStep = new JdbcStep();
				historyStep.setId(doingAction.getStepId());
				historyStep.setActionId(doingActionDescriptor.getId());
				historyStep.setCaller(caller.getUsername());
				Timestamp now = new Timestamp(new Date().getTime());
				historyStep.setStartDate(now);
				historyStep.setDueDate(now);
				historyStep.setFinishDate(now);
				historyStep.setStepName(currentStepDescriptor.getName());
				historyStep.setStepId(currentStepDescriptor.getId());
				historyStep.setUserGroup(null);
				historyStep.setWorkflowId(workflowId);
			} else {
				stmt = conn.prepareStatement(GET_CURRENT_STEPS_BY_ID_SQL);
				stmt.setLong(1, workflowId);
				stmt.setString(2, currentStepDescriptor.getId());
				rs = stmt.executeQuery();
				if (rs.next()) {
					historyStep = newJdbcStep(null, rs);
					WorkflowUtil.close(rs);
					WorkflowUtil.close(stmt);
					
					stmt = conn.prepareStatement(DELETE_CURRENT_STEP_BY_ID_SQL);
					stmt.setLong(1, workflowId);
					stmt.setLong(2, historyStep.getId());
					stmt.executeUpdate();
					WorkflowUtil.close(stmt);
				}
			}
			stmt = conn.prepareStatement(INSERT_HISTORY_STEP_SQL);
			stmt.setLong(1, historyStep.getId());
			stmt.setString(2, historyStep.getStepId());
			stmt.setString(3, historyStep.getStepName());
			stmt.setString(4, historyStep.getUserGroup());
			stmt.setString(5, caller.getUsername());
			stmt.setTimestamp(6, new Timestamp(historyStep.getStartDate().getTime()));
			stmt.setTimestamp(7, new Timestamp(new Date().getTime()));
			stmt.setTimestamp(8, new Timestamp(new Date().getTime()));
			stmt.setString(9, nextResultDescriptor.getOldStatus());
			stmt.setLong(10, workflowId);
			stmt.setString(11, doingAction.getActionId());
			stmt.setObject(12, historyStep.getPrevId());
			stmt.executeUpdate();
			WorkflowUtil.close(stmt);
			
			saveStepArgs(conn, workflowId, propertyStore, historyStep.getId(), true);
			
			if ((Boolean) inputs.get(Workflow.INITIALIZE)) {
				long stepId = historyStep.getId();
				WorkflowUtil.close(rs);
				WorkflowUtil.close(stmt);
				List<UserGroup> userGroupList = this.userGroupLoader.loadUsers(inputs, args, historyStep.getUserGroup());
				saveStepReferUser(conn, workflowId, stepId, userGroupList);
			}
			
			actionResult.setCurrentStepId(historyStep.getId());
			actionResult.setCurrentStepStepId(historyStep.getStepId());
			actionResult.setCurrentStepStatus(nextResultDescriptor.getOldStatus());
			
			if (newStepIsStopStep) {
				stmt = conn.prepareStatement(INSERT_HISTORY_STEP_SQL);
				long stepId = -System.nanoTime();
				stmt.setLong(1, stepId);
				stmt.setString(2, newStepDescriptor.getId());
				stmt.setString(3, newStepDescriptor.getName());
				stmt.setString(4, nextResultDescriptor.getUserGroup());
				stmt.setString(5, caller.getUsername());
				Timestamp now = new Timestamp(System.currentTimeMillis());
				stmt.setTimestamp(6, now);
				stmt.setTimestamp(7, now);
				stmt.setTimestamp(8, now);
				stmt.setString(9, nextResultDescriptor.getOldStatus());
				stmt.setLong(10, workflowId);
				stmt.setString(11, doingAction.getActionId());
				stmt.setObject(12, doingAction.getStepId());
				stmt.executeUpdate();
				WorkflowUtil.close(stmt);
				actionResult.setFlowStatus(FlowStatus.COMPLETED);
				actionResult.setFlowPhase(FlowPhase.ON_COMPLETE);
				stmt = conn.prepareStatement(UPDATE_WORKFLOW_STATE_SQL);
				stmt.setString(1, actionResult.getFlowStatus().toString());
				stmt.setString(2, actionResult.getFlowPhase().toString());
				stmt.setLong(3, workflowId);
				stmt.executeUpdate();
				WorkflowUtil.close(stmt);
				
				actionResult.setNewStepId(stepId);
				actionResult.setNewStepStepId(newStepDescriptor.getId());
				actionResult.setNewStepStatus(nextResultDescriptor.getOldStatus());
				
				List<UserGroup> userGroupList = this.userGroupLoader.loadUsers(inputs, args, nextResultDescriptor.getUserGroup());
				saveStepReferUser(conn, workflowId, stepId, userGroupList);
				/*最后一步，没有动作，因此删除步骤人员及输入参数日志*/
			} else {
				stmt = conn.prepareStatement(INSERT_CURRENT_STEP_SQL);
				stmt.setString(1, newStepDescriptor.getId());
				stmt.setString(2, newStepDescriptor.getName());
				stmt.setString(3, nextResultDescriptor.getUserGroup());
				stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
				stmt.setString(5, nextResultDescriptor.getStatus());
				stmt.setLong(6, workflowId);
				stmt.setObject(7, actionResult.getCurrentStepId());
				stmt.executeUpdate();
				WorkflowUtil.close(stmt);
				stmt = conn.prepareStatement(UPDATE_WORKFLOW_STATE_SQL);
				actionResult.setFlowStatus(FlowStatus.ACTIVATED);
				if (!newStepDescriptor.equals(workflowDescriptor.getStopStep())) {
					if ((Boolean) inputs.get(Workflow.INITIALIZE)) {
						actionResult.setFlowPhase(FlowPhase.ON_START);
					} else {
						actionResult.setFlowPhase(FlowPhase.ON_ACTION);
					}
				}
				stmt.setString(1, actionResult.getFlowStatus().toString());
				stmt.setString(2, actionResult.getFlowPhase().toString());
				stmt.setLong(3, workflowId);
				stmt.executeUpdate();
				WorkflowUtil.close(stmt);
				stmt = conn.prepareStatement(SELECT_INSERT_ID);
				rs = stmt.executeQuery();
				rs.next();
				long stepId = rs.getLong(1);
				WorkflowUtil.close(rs);
				WorkflowUtil.close(stmt);
				List<UserGroup> userGroupList = this.userGroupLoader.loadUsers(inputs, args, nextResultDescriptor.getUserGroup());
				saveStepReferUser(conn, workflowId, stepId, userGroupList);
				actionResult.setNewStepId(stepId);
				actionResult.setNewStepStepId(newStepDescriptor.getId());
				actionResult.setNewStepStatus(nextResultDescriptor.getStatus());
				
				/*当前步骤进入时的输入参数，没什么意义，认为不会使用，因此不做记录*/
			}
			String subflows = nextResultDescriptor.getSubflows();
			if (subflows != null) {
				String[] subflowArr = subflows.split(",");
				List<Long> subflowIds = new ArrayList<Long>(subflowArr.length);
				for (String subflow : subflowArr) {
					Map<String, Object> subInputs = new HashMap<String, Object>(inputs.size() + 1);
					subInputs.putAll(inputs);
					subInputs.put(Workflow.MAIN_FLOW_ID, workflowId);
					subInputs.put(Workflow.MAIN_FLOW_JOIN_STEP_ID, actionResult.getNewStepId());
					ActionResult subActionResult = this.startWorkflow(conn, subflow, subInputs, caller);
					subflowIds.add(subActionResult.getWorkflowId());
				}
				actionResult.setSubflowIds(subflowIds);
			}
		} catch (SQLException e) {
			throw new MitlabWorkflowException("执行工作流动作失败", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
		}
		propertyStore.clear();
		executeFunctions(inputs, args, propertyStore, nextResultDescriptor.getPostFunctions(), true);
		executeFunctions(inputs, args, propertyStore, doingActionDescriptor.getPostFunctions(), true);
		if (currentStepDescriptor != null) {
			executeFunctions(inputs, args, propertyStore, currentStepDescriptor.getPostFunctions(), true);
		}
		if (newStepIsStopStep) {
			executeFunctions(inputs, args, propertyStore, newStepDescriptor.getPostFunctions(), true);
		}
	}

	private static final String INSERT_STEP_REFER_USER_SQL = "insert t_step_user_group(step_id, refer_user, refer_user_group, workflow_id) values(?, ?, ?, ?)";
	private void saveStepReferUser(Connection conn, Long workflowId, long stepId, List<UserGroup> userGroupList) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement(INSERT_STEP_REFER_USER_SQL);
		for (UserGroup userGroup : userGroupList) {
            stmt.setLong(1, stepId);
            stmt.setString(2, userGroup.getUser());
            stmt.setString(3, userGroup.getGroup());
            stmt.setLong(4, workflowId);
            stmt.executeUpdate();
        }
		WorkflowUtil.close(stmt);
	}

	private void saveStepArgs(Connection conn, Long workflowId, Map<String, Object> inputs, Long stepId, boolean isHistoryStep) throws SQLException {
		PreparedStatement stmt = null;
		try {
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			Hessian2Output houts = hessianFactory.get().createHessian2Output(bout);
			houts.setCloseStreamOnClose(true);
			houts.setSerializerFactory(new SerializerFactory());
			try {
				houts.writeObject(inputs);
				houts.close();
			} catch (IOException e) {
				throw new MitlabWorkflowException("close houts error", e);
			}
			stmt = conn.prepareStatement(INSERT_STEP_ARGS_SQL);
			stmt.setLong(1, workflowId);
			stmt.setLong(2, stepId);
			stmt.setBytes(3, bout.toByteArray());
			stmt.setInt(4, isHistoryStep ? 1 : 0);
			stmt.executeUpdate();
		} finally {
			WorkflowUtil.close(stmt);
		}
	}

	private void executeFunctions(Map<String, Object> inputs, Map<String, Object> args, Map<String, Object> propertyStore,  FunctionsDescriptor functionsDescriptor, boolean postFunction) {
		if (functionsDescriptor != null) {
			List<FunctionDescriptor> functionDescriptorList = functionsDescriptor.getFunction();
			for (FunctionDescriptor functionDescriptor : functionDescriptorList) {
				List<ArgDescriptor> argDescriptorList = functionDescriptor.getArg();
				for (ArgDescriptor argDescriptor : argDescriptorList) {
					args.put(argDescriptor.getName(), argDescriptor.getValue());
				}
				if ("spring".equals(functionDescriptor.getType())) {
					Function function = WorkflowResolver.getInstance().getFunction((String) args.get("bean.id"));
					if (function == null) {
						throw new MitlabWorkflowException("未找到处理函数{spring:\"" + args.get("bean.id") + "\"}。");
					}
					function.execute(inputs, args, propertyStore);
					if (postFunction && !propertyStore.isEmpty()) {
						throw new MitlabWorkflowException("后处理函数{spring:\"" + args.get("bean.id") + "\"}不支持propertyStore的存储。");
					}
				} else {
					throw new MitlabWorkflowException("Unsupported Function type:" + functionDescriptor.getType());
				}
			}
		}
	}
	

	@Override
	public ActionResult doAction(long workflowId, Long stepId, String actionId, Map<String, Object> inputs, WorkflowUser caller) {
		ActionResult actionResult = new ActionResult();
		actionResult.setWorkflowId(workflowId);
		if (inputs == null) {
			inputs = new HashMap<String, Object>();
		}
		inputs.put(Workflow.INITIALIZE, false);
		Connection conn = null;
		Boolean autoCommit = null;
		try {
			conn = newConnection();
			autoCommit = WorkflowUtil.getAutoCommit(conn);
			WorkflowUtil.setAutoCommit(conn, false);
			List<Action> actions = this.getAvailableActionsWithSubflow(workflowId, stepId, inputs, caller);
			Action doingAction = null;
			for (Action action : actions) {
				if (action.getActionId().equals(actionId)) {
					doingAction = action;
					break;
				}
			}
			if (doingAction == null) {
				throw new MitlabWorkflowException("action[" + actionId +"] is invalid for workflow[" + workflowId + "]!");
			}
			this.doAction(conn, doingAction.getWorkflowId(), doingAction, inputs, WorkflowResolver.getInstance().getStepDecriptor(doingAction.getWorkflowName(), doingAction.getStepStepId()), caller, actionResult);
			WorkflowUtil.commit(conn);
			actionResult.setResult(true);
			actionResult.setDesc("success");
		}  catch (MitlabWorkflowException e) {
			actionResult.setResult(false);
			actionResult.setDesc(e.getMessage());
			logger.error(actionResult.getDesc(), e);
			WorkflowUtil.rollback(conn);
		} catch (Exception e) {
			actionResult.setResult(false);
			actionResult.setDesc("执行工作注动作失败：" + e.getMessage());
			logger.error(actionResult.getDesc(), e);
			WorkflowUtil.rollback(conn);
		}finally {
			WorkflowUtil.setAutoCommit(conn, autoCommit);
			WorkflowUtil.close(conn);
		}
		return actionResult;
	}

	private static final String GET_TODO_WORKFLOWS_SQL = "select w.workflow_id from t_workflow w, t_current_step s, t_step_user_group ug where w.workflow_id = s.workflow_id and ug.step_id = s.id  AND ug.refer_user_group = s.user_group";
	@Override
	public Set<Long> getToDoWorkflows(String workflowName, WorkflowUser caller) {
		Set<Long> workflowIds = new HashSet<Long>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = newConnection();
			stmt = conn.prepareStatement(GET_TODO_WORKFLOWS_SQL);
			//stmt.setString(1, caller.getUsername());
			rs = stmt.executeQuery();
			while (rs.next()) {
				Long workflowId = rs.getLong(1);
				if (workflowIds.contains(workflowId)) {
					continue;
				}
				workflowIds.add(workflowId);
			}
		} catch (Exception e) {
			throw new MitlabWorkflowException("查询工作流[" + workflowName + "]的待办出错", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
			WorkflowUtil.close(conn);
		}
		return workflowIds;
	}

	private static final String GET_DONE_WORKFLOWS_SQL = "select w.workflow_id from t_workflow w, t_history_step s, t_step_user_group ug where w.workflow_id = s.workflow_id and ug.step_id = s.id and ug.refer_user = ?";
	@Override
	public Set<Long> getDoneWorkflows(String workflowName, WorkflowUser caller) {
		Set<Long> workflowIds = new HashSet<Long>();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = newConnection();
			stmt = conn.prepareStatement(GET_DONE_WORKFLOWS_SQL);
			stmt.setString(1, caller.getUsername());
			rs = stmt.executeQuery();
			while (rs.next()) {
				Long workflowId = rs.getLong(1);
				if (workflowIds.contains(workflowId)) {
					continue;
				}
				workflowIds.add(workflowId);
			}
		} catch (Exception e) {
			throw new MitlabWorkflowException("查询工作流[" + workflowName + "]的已办出错", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
			WorkflowUtil.close(conn);
		}
		return workflowIds;
	}
	
	@Override
	public Map<String, Boolean> editable(long workflowId, long stepId, Map<String, Object> inputs, WorkflowUser caller) {
		return editable(workflowId, stepId, inputs, true, caller, true);
	}

	@Override
	public Map<String, Boolean> editable(long workflowId, long stepId, Map<String, Object> inputs, boolean considerAction, WorkflowUser caller) {
		return editable(workflowId, stepId, inputs, considerAction, caller, true);
	}

	private Map<String, Boolean> editable(long workflowId, Long stepId, Map<String, Object> inputs, boolean considerAction, WorkflowUser caller, boolean withSubflow) {
		Map<String, Boolean> editables = new HashMap<String, Boolean>();
		Connection conn = null;
		try {
			conn = newConnection();
			boolean passAction = false;
			if (considerAction) {
				passAction = !getAvailableActions(workflowId, stepId, inputs, caller, conn, withSubflow).isEmpty();
			}
			if (passAction) {
				List<Step> steps = this.getCurrentStepsWithSubflow(conn, workflowId, null);
				for (Step step : steps) {
					if (stepId != null) {
						if (stepId.longValue() != step.getId().longValue()) {
							continue;
						}
					}
					Map<String, String> meta = step.getMeta();
					if (meta == null) {
						continue;
					}
					Iterator<String> ikeys = meta.keySet().iterator();
					while (ikeys.hasNext()) {
						String key = ikeys.next();
						editables.put(key, Boolean.valueOf(meta.get(key)));
					}
					if (stepId != null) {
						break;
					}
				}
			}
		} finally {
			WorkflowUtil.close(conn);
		}
		return editables;
	}
	
	public List<Step> getCurrentStepsWithSubflow(long workflowId) {
		@SuppressWarnings("unchecked")
		List<Step> activatedSteps = Collections.EMPTY_LIST;
		Connection conn = null;
		try {
			conn = newConnection();
			activatedSteps = this.getCurrentStepsWithSubflow(conn, workflowId, null);
		} catch (MitlabWorkflowException e) {
			throw e;
		} finally {
			WorkflowUtil.close(conn);
		}
		return activatedSteps;
	}
	
	private static final String SELECT_SUB_WORKFLOW_IDS_SQL = "select workflow_id from t_workflow where main_flow_id = ?";
	private List<Step> getCurrentStepsWithSubflow(Connection conn, long workflowId, StepLookup stepLookup) {
		List<Step> currentSteps = getCurrentSteps(conn, workflowId, stepLookup);
		List<Long> subflowIds = getSubflowIds(conn, workflowId);
		for (Long subflowId : subflowIds) {
			currentSteps.addAll(getCurrentStepsWithSubflow(conn, subflowId, stepLookup));
		}
		return currentSteps;
	}

	public static List<Long> getSubflowIds(Connection conn, long workflowId) {
		List<Long> subflowIds = new ArrayList<Long>();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			stmt = conn.prepareStatement(SELECT_SUB_WORKFLOW_IDS_SQL);
			stmt.setLong(1, workflowId);
			rs = stmt.executeQuery();
			while (rs.next()) {
				subflowIds.add(rs.getLong(1));
			}
		} catch (SQLException e) {
			throw new MitlabWorkflowException("查询分支流程步骤失败", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
		}
		return subflowIds;
	}

}
