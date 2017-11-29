package com.mitlab.workflow;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class WorkflowTest {
    private final ApplicationContext springContext;

    public WorkflowTest() {
        Properties props = new Properties();
        props.setProperty("java.naming.factory.initial", MitlabInitialContextFactory.class.getName());
        WorkflowUtil.setInitContextEnv(props);
        springContext = new ClassPathXmlApplicationContext("spring-workflow-*.xml");
        try {
            WorkflowUtil.bindSpringConext(springContext);
            System.out.println(WorkflowUtil.lookupSpringContext());
        } catch (NamingException e) {
            e.printStackTrace();
        }
        DataSource ds = springContext.getBean(DataSource.class);
        Connection conn = null;
        Statement ps = null;
        try {
            conn = ds.getConnection();
            ps = conn.createStatement();
            ps.execute("RUNSCRIPT FROM '" + WorkflowTest.class.getResource("init.memdb.sql").getPath().substring(1) + "'");
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testNormalFlow() {
        Workflow workflow = springContext.getBean(Workflow.class);
        WorkflowUser caller = new WorkflowUser() {
            @Override
            public String getUsername() {
                return "admin";
            }

            @Override
            public boolean hasPermission(String permission) {
                if ("security:index".equals(permission)) {
                    return true;
                }
                return false;
            }
        };
        ActionResult actionResult = workflow.startWorkflow("requisition", new HashMap<String, Object>(), caller);
        assertEquals(true, actionResult.isSuccess());
        List<Step> currentSteps = workflow.getCurrentSteps(actionResult.getWorkflowId());
        assertEquals(1, currentSteps.size());
        List<Action> actions = workflow.getAvailableActions(actionResult.getWorkflowId(), new HashMap<String, Object>(), caller);
        assertNotEquals(0, actions.size());
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        actionResult = workflow.doAction(actionResult.getWorkflowId(), currentSteps.get(0).getId(), actions.get(0).getActionId(), new HashMap<String, Object>(), caller);
        List<Step> historySteps = workflow.getHistorySteps(actionResult.getWorkflowId());
        assertEquals(3, historySteps.size());
        System.out.println(historySteps.get(0).getArgs().get("time"));
        System.out.println(historySteps.get(1).getArgs().get("time"));
        //        修正SQL不规范问题，添加流程流转测试
    }
}
