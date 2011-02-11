//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.02.11 at 03:31:44 PM EST 
//


package org.voltdb.compiler.deploymentfile;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for clusterType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="clusterType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="partition-detection" type="{}partitionDetectionType" minOccurs="0"/>
 *         &lt;element name="admin-mode" type="{}adminModeType" minOccurs="0"/>
 *         &lt;element name="heartbeat" type="{}heartbeatType" minOccurs="0"/>
 *       &lt;/all>
 *       &lt;attribute name="hostcount" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="sitesperhost" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="leader" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="kfactor" type="{http://www.w3.org/2001/XMLSchema}int" default="0" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "clusterType", propOrder = {

})
public class ClusterType {

    @XmlElement(name = "partition-detection")
    protected PartitionDetectionType partitionDetection;
    @XmlElement(name = "admin-mode")
    protected AdminModeType adminMode;
    protected HeartbeatType heartbeat;
    @XmlAttribute(required = true)
    protected int hostcount;
    @XmlAttribute(required = true)
    protected int sitesperhost;
    @XmlAttribute(required = true)
    protected String leader;
    @XmlAttribute
    protected Integer kfactor;

    /**
     * Gets the value of the partitionDetection property.
     * 
     * @return
     *     possible object is
     *     {@link PartitionDetectionType }
     *     
     */
    public PartitionDetectionType getPartitionDetection() {
        return partitionDetection;
    }

    /**
     * Sets the value of the partitionDetection property.
     * 
     * @param value
     *     allowed object is
     *     {@link PartitionDetectionType }
     *     
     */
    public void setPartitionDetection(PartitionDetectionType value) {
        this.partitionDetection = value;
    }

    /**
     * Gets the value of the adminMode property.
     * 
     * @return
     *     possible object is
     *     {@link AdminModeType }
     *     
     */
    public AdminModeType getAdminMode() {
        return adminMode;
    }

    /**
     * Sets the value of the adminMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link AdminModeType }
     *     
     */
    public void setAdminMode(AdminModeType value) {
        this.adminMode = value;
    }

    /**
     * Gets the value of the heartbeat property.
     * 
     * @return
     *     possible object is
     *     {@link HeartbeatType }
     *     
     */
    public HeartbeatType getHeartbeat() {
        return heartbeat;
    }

    /**
     * Sets the value of the heartbeat property.
     * 
     * @param value
     *     allowed object is
     *     {@link HeartbeatType }
     *     
     */
    public void setHeartbeat(HeartbeatType value) {
        this.heartbeat = value;
    }

    /**
     * Gets the value of the hostcount property.
     * 
     */
    public int getHostcount() {
        return hostcount;
    }

    /**
     * Sets the value of the hostcount property.
     * 
     */
    public void setHostcount(int value) {
        this.hostcount = value;
    }

    /**
     * Gets the value of the sitesperhost property.
     * 
     */
    public int getSitesperhost() {
        return sitesperhost;
    }

    /**
     * Sets the value of the sitesperhost property.
     * 
     */
    public void setSitesperhost(int value) {
        this.sitesperhost = value;
    }

    /**
     * Gets the value of the leader property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLeader() {
        return leader;
    }

    /**
     * Sets the value of the leader property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLeader(String value) {
        this.leader = value;
    }

    /**
     * Gets the value of the kfactor property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public int getKfactor() {
        if (kfactor == null) {
            return  0;
        } else {
            return kfactor;
        }
    }

    /**
     * Sets the value of the kfactor property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setKfactor(Integer value) {
        this.kfactor = value;
    }

}
