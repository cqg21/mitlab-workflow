package com.mitlab.workflow;

public class MitlabWorkflowException extends RuntimeException {
	private static final long serialVersionUID = -1844162187199068832L;
	public MitlabWorkflowException(String message) {
		super(message);
	}
	
	public MitlabWorkflowException(String message, Throwable t) {
		super(message, t);
	}
}
