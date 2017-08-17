package com.mitlab.workflow.functions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.mitlab.workflow.WorkflowUtil;
import com.mitlab.workflow.Function;
import com.mitlab.workflow.MitlabWorkflowException;
import com.mitlab.workflow.UserGroup;
import com.mitlab.workflow.Workflow;
import com.mitlab.workflow.WorkflowUser;

public class LayoffStepUserFunction implements Function {
	private static final long serialVersionUID = 1L;

	@Override
	public void execute(Map<String, Object> inputs, Map<String, Object> args) {
		List<UserGroup> userGroupList = new ArrayList<UserGroup>();
		String group = (String) args.get("group");
		Long stepId = (Long) args.get(Workflow.ARG_STEP_ID);
		Long workflowId = (Long) args.get(Workflow.ARG_WORKFLOW_ID);
		WorkflowUser caller = (WorkflowUser) args.get(Workflow.ARG_ACTION_CALLER);
		DataSource ds = (DataSource) args.get(Workflow.ARG_WORKFLOW_DS);
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = ds.getConnection();
			stmt = conn.prepareStatement("select refer_user, refer_user_group from t_step_user_group where refer_user = ? and step_id = ? and workflow_id = ?");
			stmt.setString(1, caller.getUsername());
			stmt.setLong(2, stepId);
			stmt.setLong(3, workflowId);
			rs = stmt.executeQuery();
			while (rs.next()) {
				UserGroup ug = new UserGroup();
				ug.setUser(rs.getString(1));
				ug.setGroup(rs.getString(2));
				boolean passGroup = group == null ? true : group.equals(ug.getGroup());
				if (passGroup && ug.getUser().equals(caller.getUsername())) {
					continue;
				}
				userGroupList.add(ug);
			}
		} catch (SQLException e) {
			throw new MitlabWorkflowException("LayoffUserGroupFunction ERROR", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
			WorkflowUtil.close(conn);
			args.put(Workflow.ARG_LOADED_USER_GROUP, userGroupList);
		}
	}

}
