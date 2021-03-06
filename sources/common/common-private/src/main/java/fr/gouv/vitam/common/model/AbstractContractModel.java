/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import fr.gouv.vitam.common.parameter.ParameterHelper;

/**
 * Data Transfer Object Model of access contract (DTO).
 */

public class AbstractContractModel {

    /**
     * unique identifier
     */
    @JsonProperty("_id")
    private String id;

    /**
     * tenant id
     */
    @JsonProperty("_tenant")
    private int tenant;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("Identifier")
    private String identifier;

    @JsonProperty("Description")
    private String description;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("CreationDate")
    private String creationdate;

    @JsonProperty("LastUpdate")
    private String lastupdate;

    @JsonProperty("ActivationDate")
    private String activationdate;

    @JsonProperty("DeactivationDate")
	private String deactivationdate;



    /**
     * Constructor without fields
     * use for jackson
     */
    public AbstractContractModel() {
        setTenant(ParameterHelper.getTenantParameter());
    }

    /**
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id value to set field
     * @return this
     */
    public AbstractContractModel setId(String id) {
        this.id = id;
        return this;
    }

    /**
     * @return tenant
     */
    public int getTenant() {
        return tenant;
    }

    /**
     * @param tenant value to set working tenant
     * @return this
     */
    public AbstractContractModel setTenant(int tenant) {
        this.tenant = tenant;
        return this;
    }

    /**
     * Get the identifier of the contract
     * @return String
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the identifier of the contract
     * This value must be unique by tenant
     * @param identifier as String
     * @return this
     */
    public AbstractContractModel setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    /**
     * Get name of the contract
     * @return name as String
     */
    public String getName() {
    	return this.name;
    }

    /**
     * Set or change the contract name
     * @param name as String to set
     * @return this
     */
    public AbstractContractModel setName(String name) {
        this.name = name;
    	return this;
    }

    /**
     * Get the contract description
     * @return description of contract
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * Set or change the contract description
     * @param description to set
     * @return this
     */
    public AbstractContractModel setDescription(String description) {
        this.description = description;
    	return this;
    }

        
    /**
     * Get the contract status
     * @return status of contract
     */
    public String getStatus() {
        return this.status;
    }
    

    /**
     * Set or change the contract status
     * @param status toi set
     * @return this
     */
    public AbstractContractModel setStatus(String status) {
        this.status = status;
    	return this;
    }

    /**
     * @return the creation date of contract
     */
    public String getCreationdate() {
        return this.creationdate;
    }

    /**
     * @param creationdate to set
     * @return this
     */
    public AbstractContractModel setCreationdate(String creationdate) {
        this.creationdate = creationdate;
    	return this;
    }

    /**
     * @return last update of contract
     */
    public String getLastupdate() {
    	return this.lastupdate;
    }

    /**
     * @param lastupdate to set
     * @return this
     */
    public AbstractContractModel setLastupdate(String lastupdate) {
        this.lastupdate = lastupdate;
    	return this;
    }

    /**
     * @return the activation date of contracr
     */
    public String getActivationdate() {
    	return this.activationdate;
    }

    /**
     * @param activationdate to set
     * @return this
     */
    public AbstractContractModel setActivationdate(String activationdate) {
    	this.activationdate = activationdate;
    	return this;
    }

    /**
     * @return the desactivation date of contract
     */
    public String getDeactivationdate() {
    	return this.deactivationdate;
    }

    /**
     * @param deactivationdate to set
     * @return this
     */
    public AbstractContractModel setDeactivationdate(String deactivationdate) {
    	this.deactivationdate = deactivationdate;
    	return this;
    }

}
