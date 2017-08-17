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
import com.mitlab.workflow.JdbcWorkflow;
import com.mitlab.workflow.MitlabWorkflowException;
import com.mitlab.workflow.Step;
import com.mitlab.workflow.UserGroup;
import com.mitlab.workflow.Workflow;
import com.mitlab.workflow.descriptor.ResultDescriptor;

public class LastStepCallerFunction implements Function {
	private static final long serialVersionUID = 1L;

	@Override
	public void execute(Map<String, Object> inputs, Map<String, Object> args) {
		List<UserGroup> userGroupList = new ArrayList<UserGroup>();
		JdbcWorkflow workflow = (JdbcWorkflow) args.get(Workflow.ARG_WORKFLOW);
		Long workflowId = (Long) args.get(Workflow.ARG_WORKFLOW_ID);
		DataSource ds = (DataSource) args.get(Workflow.ARG_WORKFLOW_DS);
		ResultDescriptor resultDescriptor = (ResultDescriptor) args.get(Workflow.ARG_RESULT_DESCRIPTOR);
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = ds.getConnection();
			List<Step> steps = workflow.getHistorySteps(workflowId, conn);
			Step findStep = null;
			for (int i = steps.size() - 1; i >= 0; i--) {
				Step step = steps.get(i);
				if (step.getStepId().equals(resultDescriptor.getStep())) {
					findStep = step;
					break;
				}
			}
			if (findStep == null) {
				throw new MitlabWorkflowException("未找到最近步骤[" + resultDescriptor.getStep() + "]的操作人");
			}
			stmt = conn.prepareStatement("select refer_user, refer_user_group from t_step_user_group where refer_user = ? and step_id = ? and workflow_id = ?");
			stmt.setString(1, findStep.getCaller());
			stmt.setLong(2, findStep.getId());
			stmt.setLong(3, workflowId);
			rs = stmt.executeQuery();
			while (rs.next()) {
				UserGroup ug = new UserGroup();
				ug.setUser(rs.getString(1));
				ug.setGroup(rs.getString(2));
				userGroupList.add(ug);
			}
		} catch (SQLException e) {
			throw new MitlabWorkflowException("LastStepCallerFunction ERROR", e);
		} finally {
			WorkflowUtil.close(rs);
			WorkflowUtil.close(stmt);
			WorkflowUtil.close(conn);
			args.put(Workflow.ARG_LOADED_USER_GROUP, userGroupList);
		}
	}

}
