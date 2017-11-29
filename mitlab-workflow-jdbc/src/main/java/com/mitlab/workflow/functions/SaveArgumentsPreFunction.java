package com.mitlab.workflow.functions;

import com.mitlab.workflow.Function;
import com.mitlab.workflow.UserGroup;
import com.mitlab.workflow.Workflow;
import com.mitlab.workflow.WorkflowUser;
import com.mitlab.workflow.descriptor.ResultDescriptor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SaveArgumentsPreFunction implements Function {
	private static final long serialVersionUID = 1L;

	@Override
	public void execute(Map<String, Object> inputs, Map<String, Object> args, Map<String, Object> propertyStore) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    propertyStore.put("time", dateFormat.format(new Date()));
	}

}
