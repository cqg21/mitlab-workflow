package com.mitlab.workflow;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.mitlab.workflow.descriptor.ActionDescriptor;
import com.mitlab.workflow.descriptor.ResultDescriptor;
import com.mitlab.workflow.descriptor.StepDescriptor;
import com.mitlab.workflow.descriptor.WorkflowDescriptor;

public class WorkflowResolver {
	private final Log logger = LogFactory.getLog(this.getClass());
	
	private ApplicationContext springContext;
	private static final Map<String, WorkflowDescriptor> workflowDescriptorMapping = new HashMap<String, WorkflowDescriptor>();
	private static final Map<String, Map<String, StepDescriptor>> stepDescriptorMapping = new HashMap<String, Map<String, StepDescriptor>>();
	private static final Map<String, Function> beanFunctionMapping = new HashMap<String, Function>();
	private static final Map<String, Condition> beanConditionMapping = new HashMap<String, Condition>();
	private static final WorkflowResolver INSTANCE = new WorkflowResolver();
	public static void main(String[] args) {
		WorkflowResolver.getInstance();
	}
	
	private void closeStream(Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	private WorkflowResolver() {
		try {
			springContext = WorkflowUtil.lookupSpringContext();
			final PathMatchingResourcePatternResolver pmrpr = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());
			Resource[] workflowResources = pmrpr.getResources("classpath*:workflows/*.xml");
			JAXBContext jaxbc = JAXBContext.newInstance("com.mitlab.workflow.descriptor");
			Unmarshaller unmarshaller = jaxbc.createUnmarshaller();
			for (Resource workflowResource : workflowResources) {
				InputStream ins = workflowResource.getInputStream();
				@SuppressWarnings("unchecked")
				JAXBElement<WorkflowDescriptor> workflowElement = (JAXBElement<WorkflowDescriptor>) unmarshaller.unmarshal(ins);
				WorkflowDescriptor workflowDescriptor = (WorkflowDescriptor) workflowElement.getValue();
				this.closeStream(ins);
				workflowDescriptorMapping.put(workflowDescriptor.getName(), workflowDescriptor);
				Map<String, StepDescriptor> sdm = new HashMap<String, StepDescriptor>();
				stepDescriptorMapping.put(workflowDescriptor.getName(), sdm);
				sdm.put(workflowDescriptor.getStartStep().getId(), workflowDescriptor.getStartStep());
				List<StepDescriptor> steps = workflowDescriptor.getStep();
				for (StepDescriptor step : steps) {
					sdm.put(step.getId(), step);
				}
				sdm.put(workflowDescriptor.getStopStep().getId(), workflowDescriptor.getStopStep());
			}
		} catch (NamingException e) {
			throw new MitlabWorkflowException("初始化工作流JNDI出错", e);
		} catch (IOException e) {
			throw new MitlabWorkflowException("加载工作流配置文件出错", e);
		} catch (JAXBException e) {
			throw new MitlabWorkflowException("加载工作流配置文件出错", e);
		}
		beanFunctionMapping.putAll(springContext.getBeansOfType(Function.class));
		beanConditionMapping.putAll(springContext.getBeansOfType(Condition.class));
	}
	
	public Function getFunction(String function) {
		return beanFunctionMapping.get(function);
	}
	
	public Condition getCondition(String condition) {
		return beanConditionMapping.get(condition);
	}
	
	public WorkflowDescriptor getWorkflowDescriptor(String workflowName) {
		return workflowDescriptorMapping.get(workflowName);
	}
	
	public StepDescriptor getStepDecriptor(String workflowName, String stepId) {
		Map<String, StepDescriptor> sdm = stepDescriptorMapping.get(workflowName);
		if (sdm == null) {
			return null;
		}
		return sdm.get(stepId);
	}
	
	/**
	 * graph TB
     * A((Circle)) -- Link text --> B(Round Rect)
     * A --> C(Round Rect)
     * B --> D{Rhombus}
     * C --> D
	 */
	public String verticalFlowGraph(String workflowName, List<Step> currentSteps) {
		return printFlowGraph("graph TB", workflowName, currentSteps);
	}
	
	public String verticalFlowGraph(String workflowName) {
		return printFlowGraph("graph TB", workflowName, null);
	}
	
	public String horizontalFlowGraph(String workflowName, List<Step> currentSteps) {
		return printFlowGraph("graph LR", workflowName, currentSteps);
	}
	
	public String horizontalFlowGraph(String workflowName) {
		return printFlowGraph("graph LR", workflowName, null);
	}
	
	private String printFlowGraph(String graph, String workflowName, List<Step> currentSteps) {
		StringBuilder json = new StringBuilder();
		if (graph != null) {
			json.append(graph).append("\n");
		}
		json.append(printFlowGraph(workflowName, true));
		if (currentSteps != null && !currentSteps.isEmpty()) {
			json.append("classDef actionActivated fill:#9f6,stroke:#333,stroke-width:1px;").append("\n");
			json.append("class ");
			for (Step step : currentSteps) {
				json.append("W-").append(step.getWorkflowName()).append("-STEP-").append(step.getStepId()).append(",");
			}
			json.deleteCharAt(json.length() - 1);
			json.append(" actionActivated");
		}
		return json.toString();
	}

	private String printFlowGraph(String workflowName, boolean mainFlow) {
		WorkflowDescriptor wd = WorkflowResolver.getInstance().getWorkflowDescriptor(workflowName);
		List<StepDescriptor> sds = new ArrayList<StepDescriptor>(wd.getStep().size() + 2);
		sds.add(wd.getStartStep());
		sds.addAll(wd.getStep());
		sds.add(wd.getStopStep());
		Map<String, StepDescriptor> stepMapping = new HashMap<String, StepDescriptor>();
		for (int si = 0; si < sds.size(); si++) {
			StepDescriptor sd = sds.get(si);
			stepMapping.put(sd.getId(), sd);
		}
		List<String> subflowList = new ArrayList<String>();
		StringBuilder json = new StringBuilder();
		if (mainFlow) {
			json.append("subgraph 主流程\n");
		}
		for (int si = 0; si < sds.size(); si++) {
			StepDescriptor sd = sds.get(si);
			if (sd.getActions() == null) {
				continue;
			}
			List<ActionDescriptor> ads = sd.getActions().getAction();
			for (int ai = 0; ai < ads.size(); ai++) {
				ActionDescriptor ad = ads.get(ai);
				List<ResultDescriptor> rs = ad.getResults().getResult();
				final boolean isStartNode = wd.getStartStep().equals(sd);
				final boolean hasMoreThanOneResult = rs.size() > 1;
				if (hasMoreThanOneResult) {
					json.append("W-").append(workflowName).append("-STEP-").append(sd.getId()).append(isStartNode ? "((" : "(").append(isStartNode ? "起点": sd.getName()).append(isStartNode ? "))" : ")");
					json.append("-->").append("W-").append(workflowName).append("-ACT-").append(ad.getId()).append("{").append(ad.getName()).append("}").append("\n");
				}
				for (ResultDescriptor r : rs) {
					String[] subflows = r.getSubflows() == null ? new String[0] : r.getSubflows().split(",");
					if (hasMoreThanOneResult) {
						for (String subflow : subflows) {
							WorkflowDescriptor subWD = this.getWorkflowDescriptor(subflow);
							StringBuilder subJson = new StringBuilder();
							subJson.append("subgraph 分支流程\n");
							subJson.append("W-").append(workflowName).append("-ACT-").append(ad.getId()).append("{").append(ad.getName()).append("}");
							subJson.append("-->|split|").append("W-").append(subWD.getName()).append("-STEP-").append(subWD.getStartStep().getId()).append("((起点))").append("\n");
							
							subJson.append(printFlowGraph(subflow, false));
							
							subJson.append("W-").append(subWD.getName()).append("-STEP-").append(subWD.getStopStep().getId()).append("((终点))").append("-->|join|");
							StepDescriptor ns = stepMapping.get(r.getStep());
							if (ns == null) {
								throw new MitlabWorkflowException("流程配置中未找到步骤：" + r.getStep());
							}
							boolean isLastNode = wd.getStopStep().equals(stepMapping.get(r.getStep()));
							subJson.append("W-").append(workflowName).append("-STEP-").append(ns.getId()).append(isLastNode ? "((" : "(").append(isLastNode ? "终点": ns.getName()).append(isLastNode ? "))" : ")").append("\n");
							
							subJson.append("end").append("\n");
							subflowList.add(subJson.toString());
						}
						json.append("W-").append(workflowName).append("-ACT-").append(ad.getId()).append("{").append(ad.getName()).append("}");
						if (r.getName() != null && !"".equals(r.getName())) {
							json.append("-.->|").append(r.getName()).append("|");
						} else {
							json.append("-.->");
						}
					} else {
						for (String subflow : subflows) {
							WorkflowDescriptor subWD = this.getWorkflowDescriptor(subflow);
							StringBuilder subJson = new StringBuilder();
							subJson.append("subgraph 分支流程\n");
							subJson.append("W-").append(workflowName).append("-STEP-").append(sd.getId()).append(isStartNode ? "((" : "(").append(isStartNode ? "起点": sd.getName()).append(isStartNode ? "))" : ")");
							subJson.append("-->|split|").append("W-").append(subWD.getName()).append("-STEP-").append(subWD.getStartStep().getId()).append("((起点))").append("\n");
							
							subJson.append(printFlowGraph(subflow, false));
							
							subJson.append("W-").append(subWD.getName()).append("-STEP-").append(subWD.getStopStep().getId()).append("((终点))").append("-->|join|");
							StepDescriptor ns = stepMapping.get(r.getStep());
							if (ns == null) {
								throw new MitlabWorkflowException("流程配置中未找到步骤：" + r.getStep());
							}
							boolean isLastNode = wd.getStopStep().equals(stepMapping.get(r.getStep()));
							subJson.append("W-").append(workflowName).append("-STEP-").append(ns.getId()).append(isLastNode ? "((" : "(").append(isLastNode ? "终点": ns.getName()).append(isLastNode ? "))" : ")").append("\n");
							
							subJson.append("end").append("\n");
							subflowList.add(subJson.toString());
						}
						
						json.append("W-").append(workflowName).append("-STEP-").append(sd.getId()).append(isStartNode ? "((" : "(").append(isStartNode ? "起点": sd.getName()).append(isStartNode ? "))" : ")");
						json.append("--").append(ad.getName()).append("-->");
					}
					StepDescriptor ns = stepMapping.get(r.getStep());
					if (ns == null) {
						throw new MitlabWorkflowException("流程配置中未找到步骤：" + r.getStep());
					}
					boolean isLastNode = wd.getStopStep().equals(stepMapping.get(r.getStep()));
					json.append("W-").append(workflowName).append("-STEP-").append(ns.getId()).append(isLastNode ? "((" : "(").append(isLastNode ? "终点": ns.getName()).append(isLastNode ? "))" : ")").append("\n");
				}
			}
		}
		if (mainFlow) {
			json.append("end\n");
		}
		for (String subflow : subflowList) {
			json.append(subflow);
		}
		return json.toString();
	}
	
	public static final WorkflowResolver getInstance() {
		return INSTANCE;
	}
}
