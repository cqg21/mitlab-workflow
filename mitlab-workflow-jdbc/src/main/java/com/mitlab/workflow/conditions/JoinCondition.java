package com.mitlab.workflow.conditions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.mitlab.workflow.Condition;
import com.mitlab.workflow.WorkflowUtil;
import com.mitlab.workflow.FlowStatus;
import com.mitlab.workflow.MitlabWorkflowException;
import com.mitlab.workflow.Workflow;
import com.mitlab.workflow.WorkflowUser;

public class JoinCondition implements Condition {
	private static final long serialVersionUID = 1L;

	@Override
	public boolean passesCondition(Map<String, Object> inputs, Map<String, Object> args, WorkflowUser caller) {
		boolean passCondition = true;
		DataSource ds = (DataSource) args.get(Workflow.ARG_WORKFLOW_DS);
		Long workflowId = (Long) args.get(Workflow.ARG_WORKFLOW_ID);
		Long stepId = (Long) args.get(Workflow.ARG_STEP_ID);
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = ds.getConnection();
			stmt = conn.prepareStatement("select workflow_id from t_workflow where main_flow_id = ? and main_flow_join_step_id = ?");
			stmt.setLong(1, workflowId);
			stmt.setLong(2, stepId);
			rs = stmt.executeQuery();
			List<Long> subflowIds = new ArrayList<Long>();
			while (rs.next()) {
				subflowIds.add(rs.getLong(1));
			}
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
			if (!subflowIds.isEmpty()) {
				StringBuilder sql = new StringBuilder("select workflow_status from t_workflow where workflow_id in(");
				int maxIdxOffset = subflowIds.size() - 1;
				for (int i = 0; i <= maxIdxOffset; i++) {
					sql.append(subflowIds.get(i));
					if (i < maxIdxOffset) {
						sql.append(",");
					}
				}
				sql.append(")");
				stmt = conn.prepareStatement(sql.toString());
				rs = stmt.executeQuery();
				while (rs.next()) {
					passCondition = FlowStatus.value(rs.getString(1)) == FlowStatus.COMPLETED;
					if (!passCondition) {
						break;
					}
				}
				WorkflowUtil.close(rs);
				WorkflowUtil.close(stmt);
			}
		} catch (SQLException e) {
			throw new MitlabWorkflowException("JoinCondition DBConnection Error", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
			WorkflowUtil.close(conn);
		}
		return passCondition;
	}

}
