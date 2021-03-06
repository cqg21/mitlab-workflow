//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.06.29 at 04:24:55 PM CST 
//


package com.mitlab.workflow.descriptor;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for WorkflowDescriptor complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WorkflowDescriptor">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="start-step" type="{http://www.mitlab.org/workflow}StepDescriptor"/>
 *         &lt;element name="step" type="{http://www.mitlab.org/workflow}StepDescriptor" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="stop-step" type="{http://www.mitlab.org/workflow}StepDescriptor"/>
 *       &lt;/sequence>
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WorkflowDescriptor", propOrder = {
    "startStep",
    "step",
    "stopStep"
})
public class WorkflowDescriptor {

    @XmlElement(name = "start-step", required = true)
    protected StepDescriptor startStep;
    protected List<StepDescriptor> step;
    @XmlElement(name = "stop-step", required = true)
    protected StepDescriptor stopStep;
    @XmlAttribute(name = "name")
    protected String name;

    /**
     * Gets the value of the startStep property.
     * 
     * @return
     *     possible object is
     *     {@link StepDescriptor }
     *     
     */
    public StepDescriptor getStartStep() {
        return startStep;
    }

    /**
     * Sets the value of the startStep property.
     * 
     * @param value
     *     allowed object is
     *     {@link StepDescriptor }
     *     
     */
    public void setStartStep(StepDescriptor value) {
        this.startStep = value;
    }

    /**
     * Gets the value of the step property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the step property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getStep().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link StepDescriptor }
     * 
     * 
     */
    public List<StepDescriptor> getStep() {
        if (step == null) {
            step = new ArrayList<StepDescriptor>();
        }
        return this.step;
    }

    /**
     * Gets the value of the stopStep property.
     * 
     * @return
     *     possible object is
     *     {@link StepDescriptor }
     *     
     */
    public StepDescriptor getStopStep() {
        return stopStep;
    }

    /**
     * Sets the value of the stopStep property.
     * 
     * @param value
     *     allowed object is
     *     {@link StepDescriptor }
     *     
     */
    public void setStopStep(StepDescriptor value) {
        this.stopStep = value;
    }

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

}
