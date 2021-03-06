package com.mitlab.workflow;

import java.io.Serializable;
import java.util.Map;

public interface Function extends Serializable {
	void execute(Map<String, Object> inputs, Map<String, Object> args, Map<String, Object> propertyStore);
}
