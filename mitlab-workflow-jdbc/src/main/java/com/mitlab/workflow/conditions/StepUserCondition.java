package com.mitlab.workflow.conditions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import com.mitlab.workflow.Condition;
import com.mitlab.workflow.WorkflowUtil;
import com.mitlab.workflow.MitlabWorkflowException;
import com.mitlab.workflow.Workflow;
import com.mitlab.workflow.WorkflowUser;

public class StepUserCondition implements Condition {
	private static final long serialVersionUID = 1L;

	@Override
	public boolean passesCondition(Map<String, Object> inputs, Map<String, Object> args, WorkflowUser caller) {
		boolean passCondition = false;
		DataSource ds = (DataSource) args.get(Workflow.ARG_WORKFLOW_DS);
		Long workflowId = (Long) args.get(Workflow.ARG_WORKFLOW_ID);
		Long stepId = (Long) args.get(Workflow.ARG_STEP_ID);
		String group = (String) args.get("group");
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			conn = ds.getConnection();
			if (group == null) {
				stmt = conn.prepareStatement("select 'x' from t_step_user_group where workflow_id = ? and step_id = ? and refer_user = ?");
				stmt.setLong(1, workflowId);
				stmt.setLong(2, stepId);
				stmt.setString(3, caller.getUsername());
			} else {
				stmt = conn.prepareStatement("select 'x' from t_step_user_group where workflow_id = ? and step_id = ? and refer_user = ? and refer_user_group = ?");
				stmt.setLong(1, workflowId);
				stmt.setLong(2, stepId);
				stmt.setString(3, caller.getUsername());
				stmt.setString(4, group);
			}
			rs = stmt.executeQuery();
			passCondition = rs.next();
		} catch (SQLException e) {
			throw new MitlabWorkflowException("UserGroupCondition DBConnection Error", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
			WorkflowUtil.close(conn);
		}
		return passCondition;
	}

}
