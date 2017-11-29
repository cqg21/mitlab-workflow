package com.mitlab.workflow.functions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.mitlab.workflow.WorkflowUtil;
import com.mitlab.workflow.FlowPhase;
import com.mitlab.workflow.FlowStatus;
import com.mitlab.workflow.Function;
import com.mitlab.workflow.JdbcWorkflow;
import com.mitlab.workflow.MitlabWorkflowException;
import com.mitlab.workflow.Step;
import com.mitlab.workflow.Workflow;
import com.mitlab.workflow.WorkflowUser;

public class KillSubflowFunction implements Function {
	private static final long serialVersionUID = 2115156325092104210L;

	private static final String DELETE_CURRENT_STEP_BY_ID_SQL = "delete from t_current_step where workflow_id = ? and id = ?";
	private static final String INSERT_HISTORY_STEP_SQL = "insert t_history_step(id, step_id, step_name, user_group, caller, start_date, due_date, finish_date, status, workflow_id, action_id, prev_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String UPDATE_WORKFLOW_STATE_SQL = "update t_workflow set workflow_status = ?, workflow_phase = ? where workflow_id = ?";
	@Override
	public void execute(Map<String, Object> inputs, Map<String, Object> args, Map<String, Object> propertyStore) {
		final Long workflowId = (Long) args.get(Workflow.ARG_WORKFLOW_ID);
		final DataSource ds = (DataSource) args.get(Workflow.ARG_WORKFLOW_DS);
		final WorkflowUser caller = (WorkflowUser) args.get(Workflow.ARG_ACTION_CALLER);
		Connection conn = null;
		PreparedStatement stmt = null;
		// 子流程
		try {
			conn = ds.getConnection();
			List<Long> subflowIds = JdbcWorkflow.getSubflowIds(conn, workflowId);
			Timestamp now = new Timestamp(System.nanoTime());
			for (Long subflowId : subflowIds) {
				List<Step> subSteps = JdbcWorkflow.getCurrentSteps(conn, subflowId, null);
				for (Step subStep : subSteps) {
					stmt = conn.prepareStatement(DELETE_CURRENT_STEP_BY_ID_SQL);
					stmt.setLong(1, subflowId);
					stmt.setLong(2, subStep.getId());
					stmt.executeUpdate();
					WorkflowUtil.close(stmt);
					stmt = conn.prepareStatement(INSERT_HISTORY_STEP_SQL);
					stmt.setLong(1, subStep.getId());
					stmt.setString(2, subStep.getStepId());
					stmt.setString(3, subStep.getStepName());
					stmt.setString(4, FlowStatus.KILLED.toString());
					stmt.setString(5, caller.getUsername());
					stmt.setTimestamp(6, now);
					stmt.setTimestamp(7, now);
					stmt.setTimestamp(8, now);
					stmt.setString(9, FlowStatus.KILLED.toString());
					stmt.setLong(10, subflowId);
					stmt.setString(11, null);
					stmt.setObject(12, -System.nanoTime());
					stmt.executeUpdate();
					WorkflowUtil.close(stmt);
				}
				stmt = conn.prepareStatement(UPDATE_WORKFLOW_STATE_SQL);
				stmt.setString(1, FlowStatus.KILLED.toString());
				stmt.setString(2, FlowPhase.ON_COMPLETE.toString());
				stmt.setLong(3, subflowId);
				stmt.executeUpdate();
				WorkflowUtil.close(stmt);
			}
		} catch (SQLException e) {
			throw new MitlabWorkflowException("kill subflow error", e);
		} finally {
			WorkflowUtil.close(stmt);
			WorkflowUtil.close(conn);
		}
	}

}
