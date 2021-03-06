package com.mitlab.workflow;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;

public class WorkflowUtil {
	private static final Log logger = LogFactory.getLog(WorkflowUtil.class);
	private static final Properties PROPS = new Properties();

	public static final void setInitContextEnv(Properties props) {
        Set<String> keys = props.stringPropertyNames();
        for (String key : keys) {
            PROPS.setProperty(key, props.getProperty(key));
        }
    }
	
	public static final void bindSpringConext(ApplicationContext applicationContext) throws NamingException {
		InitialContext initContext = new InitialContext(PROPS);
		initContext.bind("__mitlab_spring_context", applicationContext);
	}
	
	public static final ApplicationContext lookupSpringContext()  throws NamingException {
        InitialContext initContext = new InitialContext(PROPS);
		return (ApplicationContext) initContext.lookup("__mitlab_spring_context");
	}
	
	public static void close(Connection conn) {
		if (conn == null) {
			return;
		}
		try {
            conn.close();
		} catch (Throwable e) {
			logger.error("close mitlab-workflow conn error", e);
		}
	}

	public static void close(Statement stmt) {
		if (stmt == null) {
			return;
		}
		try {
            stmt.close();
		} catch (Throwable e) {
			logger.error("close mitlab-workflow stmt error", e);
		}
	}

	public static void close(ResultSet rs) {
		if (rs == null) {
			return;
		}
		try {
            rs.close();
		} catch (Throwable e) {
			logger.error("close mitlab-workflow rs error", e);
		}
	}
	
	public static void setAutoCommit(Connection conn, Boolean autoCommit) {
		if (conn == null || autoCommit == null) {
			return;
		}
		try {
			conn.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			logger.error("设置AutoCommit出错", e);
		}
	}

	public static Boolean getAutoCommit(Connection conn) {
		if (conn == null) {
			return null;
		}
		Boolean autoCommit = null;
		try {
			autoCommit = conn.getAutoCommit();
		} catch (SQLException e) {
			logger.error("获取AutoCommit出错", e);
		}
		return autoCommit;
	}

	public static void commit(Connection conn) {
		if (conn == null) {
			return;
		}
		try {
			conn.commit();
		} catch (SQLException e) {
			logger.error("提交事务出错", e);
		}
	}

	public static void rollback(Connection conn) {
		if (conn == null) {
			return;
		}
		try {
			conn.rollback();
		} catch (SQLException e) {
			logger.error("回滚事务出错", e);
		}
	}
}
