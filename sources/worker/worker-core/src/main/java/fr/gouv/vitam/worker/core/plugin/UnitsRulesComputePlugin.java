/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.plugin;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.or;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.RuleMeasurementEnum;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.IngestWorkflowConstants;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * UnitsRulesCompute Plugin.<br>
 *
 */

public class UnitsRulesComputePlugin extends ActionHandler {
    private static final String WORKSPACE_SERVER_ERROR = "Workspace Server Error";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(UnitsRulesComputePlugin.class);

    private static final String CHECK_RULES_TASK_ID = "UNITS_RULES_COMPUTE";
    private static final String FILE_COULD_NOT_BE_DELETED_MSG = "File could not be deleted";
    private static final String AU_PREFIX_WITH_END_DATE = "WithEndDte_";
    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";
    private static final String CHECKS_RULES = "Rules checks problem: missing parameters";
    private static final String UNLIMITED_RULE_DURATION = "unlimited";
    private static final String NON_EXISTING_RULE = "Rule %s does not exist";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT_PATTERN);

    private HandlerIO handlerIO;

    /**
     * Empty constructor UnitsRulesComputePlugin
     *
     */
    public UnitsRulesComputePlugin() {
        // Empty
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handler) {
        LOGGER.debug("UNITS_RULES_COMPUTE in execute");
        final long time = System.currentTimeMillis();
        handlerIO = handler;
        final ItemStatus itemStatus = new ItemStatus(CHECK_RULES_TASK_ID);
        try {
            calculateMaturityDate(params, itemStatus);
            itemStatus.increment(StatusCode.OK);
        } catch (final ProcessingException e) {
            LOGGER.debug(e);
            itemStatus.increment(StatusCode.KO);
        }


        LOGGER.debug("[exit] execute... /Elapsed Time:" + (System.currentTimeMillis() - time) / 1000 + "s");
        return new ItemStatus(CHECK_RULES_TASK_ID).setItemsStatus(CHECK_RULES_TASK_ID, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing to check
    }


    private void calculateMaturityDate(WorkerParameters params, ItemStatus itemStatus) throws ProcessingException {
        ParameterHelper.checkNullOrEmptyParameters(params);
        final String containerId = params.getContainerName();
        final String objectName = params.getObjectName();

        try (InputStream inputStream =
            handlerIO.getInputStreamFromWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + objectName)) {
            // Parse RULES in management Archive unit, and add EndDate
            // parseXmlRulesAndUpdateEndDate(inputStream, objectName, containerId, params, itemStatus);
            parseRulesAndUpdateEndDate(inputStream, objectName, containerId);
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException |
            IOException e) {
            LOGGER.error(WORKSPACE_SERVER_ERROR, e);
            throw new ProcessingException(e);
        }
    }

    /**
     * findRulesValueQueryBuilders: select query
     *
     * @param rulesId
     * @return the JsonNode answer
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     * @throws AdminManagementClientServerException
     * @throws ProcessingException
     */

    private JsonNode findRulesValueQueryBuilders(Set<String> rulesId)
        throws InvalidCreateOperationException, InvalidParseOperationException,
        IOException, ProcessingException {
        final Select select =
            new Select();
        select.addOrderByDescFilter(FileRules.RULEID);
        final BooleanQuery query = or();
        for (final String ruleId : rulesId) {
            query.add(eq(FileRules.RULEID, ruleId));
        }
        select.setQuery(query);

        try (AdminManagementClient adminManagementClient = AdminManagementClientFactory.getInstance().getClient()) {
            return adminManagementClient.getRules(select.getFinalSelect());
        } catch (final VitamException e) {
            throw new ProcessingException(e);
        }

    }

    /**
     * Check archiveUnit json file and add end date for rules.
     * 
     * @param input archiveUnit json file
     * @param objectName json file name
     * @param containerName
     * @throws IOException
     * @throws ProcessingException
     */
    private void parseRulesAndUpdateEndDate(InputStream input, String objectName, String containerName)
        throws IOException, ProcessingException {

        final File fileWithEndDate = handlerIO.getNewLocalFile(AU_PREFIX_WITH_END_DATE + objectName);
        try {

            // Archive unit nodes
            JsonNode archiveUnit = JsonHandler.getFromInputStream(input);
            JsonNode archiveUnitNode = archiveUnit.get(SedaConstants.TAG_ARCHIVE_UNIT);
            JsonNode workNode = archiveUnit.get(SedaConstants.PREFIX_WORK);
            JsonNode managementNode = archiveUnitNode.get(SedaConstants.TAG_MANAGEMENT);

            // temp data
            JsonNode rulesResults;

            // rules to apply
            Set<String> rulesToApply = new HashSet<>();
            if (workNode.get(SedaConstants.TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT) != null) {
                if (workNode.get(SedaConstants.TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT).isArray()) {
                    // FIXME replace with always real arrayNode
                    ArrayNode rulesToApplyArray =
                        (ArrayNode) workNode.get(SedaConstants.TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT);
                    if (rulesToApplyArray.size() > 0) {
                        rulesToApply = Sets
                            .newHashSet(Splitter.on(SedaConstants.RULE_SEPARATOR)
                                .split(rulesToApplyArray.iterator().next().asText()));
                    }
                } else {
                    rulesToApply = Sets
                        .newHashSet(Splitter.on(SedaConstants.RULE_SEPARATOR)
                            .split(workNode.get(SedaConstants.TAG_RULE_APPLING_TO_ROOT_ARCHIVE_UNIT).asText()));
                }
            }
            if (rulesToApply.isEmpty()) {
                LOGGER.debug("Archive unit does not have rules");
                return;
            }

            // search ref rules
            rulesResults = findRulesValueQueryBuilders(rulesToApply);
            LOGGER.debug("rulesResults for archive unit id: " + objectName +
                " && containerName is :" + containerName + " is:" + rulesResults);

            // update all rules
            for (String ruleType : SedaConstants.getSupportedRules()) {
                JsonNode ruleTypeNode = managementNode.get(ruleType);
                if (ruleTypeNode == null || ruleTypeNode.size() == 0 ||
                    ruleTypeNode.findValues(SedaConstants.TAG_RULE_RULE).size() == 0) {
                    LOGGER.debug("no rules of type " + ruleType + " found");
                    continue;
                }
                if (ruleTypeNode.isArray()) {
                    ArrayNode ruleNodes = (ArrayNode) ruleTypeNode;
                    for (JsonNode ruleNode : ruleNodes) {
                        computeRuleNode((ObjectNode) ruleNode, rulesResults, ruleType);
                    }
                } else {
                    LOGGER.debug("ruleTypeNode of type " + ruleType + " should be an array");
                    throw new ProcessingException("ruleTypeNode should be an array");
                }

            }
            JsonHandler.writeAsFile(archiveUnit, fileWithEndDate);
        } catch (InvalidParseOperationException | InvalidCreateOperationException | FileRulesException |
            ParseException e) {
            LOGGER.error(e);
            throw new ProcessingException(e);
        }

        // Write to workspace
        try {
            handlerIO.transferFileToWorkspace(IngestWorkflowConstants.ARCHIVE_UNIT_FOLDER + "/" + objectName,
                fileWithEndDate, true);
        } catch (final ProcessingException e) {
            LOGGER.error("Can not write to workspace ", e);
            if (!fileWithEndDate.delete()) {
                LOGGER.warn(FILE_COULD_NOT_BE_DELETED_MSG);
            }
            throw e;
        }
    }

    /**
     * Compute enddate for rule node
     * 
     * @param ruleNode current ruleNode from archive unit
     * @param rulesResults rules referential
     * @param ruleType current rule type
     * @throws FileRulesException
     * @throws InvalidParseOperationException
     * @throws ProcessingException
     * @throws ParseException
     */
    private void computeRuleNode(ObjectNode ruleNode, JsonNode rulesResults, String ruleType)
        throws FileRulesException, InvalidParseOperationException, ProcessingException, ParseException {
        String ruleId = ruleNode.get(SedaConstants.TAG_RULE_RULE).asText();
        String startDate = "";

        if (getRuleNodeByID(ruleId, ruleType, rulesResults) == null) {
            String errorMessage = String.format(NON_EXISTING_RULE, ruleId);
            throw new ProcessingException(errorMessage);
        }

        if (ruleNode.get(SedaConstants.TAG_RULE_START_DATE) != null) {
            startDate = ruleNode.get(SedaConstants.TAG_RULE_START_DATE).asText();
        }
        LocalDate endDate = getEndDate(startDate, ruleId, rulesResults, ruleType);
        if (endDate != null) {
            ruleNode.put(SedaConstants.TAG_RULE_END_DATE, endDate.format(DATE_TIME_FORMATTER));
        }
    }


    private JsonNode getRuleNodeByID(String ruleId, String ruleType, JsonNode jsonResult) {
        if (jsonResult != null && ParametersChecker.isNotEmpty(ruleId, ruleType)) {
            final ArrayNode rulesResult = (ArrayNode) jsonResult.get("$results");
            for (final JsonNode rule : rulesResult) {
                if (rule.get(FileRules.RULEID) != null && rule.get(FileRules.RULETYPE) != null) {
                    final String ruleIdFromList = rule.get(FileRules.RULEID).asText();
                    final String ruleTypeFromList = rule.get(FileRules.RULETYPE).asText();
                    if (ruleId.equals(ruleIdFromList) && ruleType.equals(ruleTypeFromList)) {
                        return rule;
                    }
                }
            }
        }
        return null;
    }

    private LocalDate getEndDate(String startDateString, String ruleId, JsonNode rulesResults, String currentRuleType)
        throws FileRulesException, InvalidParseOperationException, ParseException, ProcessingException {

        if (!ParametersChecker.isNotEmpty(startDateString)) {
            return null;
        }
        if (ParametersChecker.isNotEmpty(ruleId, currentRuleType)) {

            LocalDate startDate = LocalDate.parse(startDateString, DATE_TIME_FORMATTER);

            final JsonNode ruleNode = getRuleNodeByID(ruleId, currentRuleType, rulesResults);

            if (checkRulesParameters(ruleNode)) {
                final String duration = ruleNode.get(FileRules.RULEDURATION).asText();
                final String measurement = ruleNode.get(FileRules.RULEMEASUREMENT).asText();
                if (duration.equalsIgnoreCase(UNLIMITED_RULE_DURATION)) {
                    return null;
                }
                final RuleMeasurementEnum ruleMeasurement = RuleMeasurementEnum.getEnumFromType(measurement);
                return startDate.plus(Integer.parseInt(duration), ruleMeasurement.getTemporalUnit());
            } else {
                throw new ProcessingException(CHECKS_RULES);
            }
        }
        return null;
    }

    /**
     * @param ruleNode
     */
    private boolean checkRulesParameters(JsonNode ruleNode) {
        return ruleNode != null && ruleNode.get(FileRules.RULEDURATION) != null &&
            ruleNode.get(FileRules.RULEMEASUREMENT) != null;
    }

}
