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
package fr.gouv.vitam.worker.core.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCyclesClientHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookType;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.processing.common.exception.HandlerNotFoundException;
import fr.gouv.vitam.processing.common.exception.PluginNotFoundException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.Action;
import fr.gouv.vitam.processing.common.model.ActionDefinition;
import fr.gouv.vitam.processing.common.model.DistributionKind;
import fr.gouv.vitam.processing.common.model.ProcessBehavior;
import fr.gouv.vitam.processing.common.model.Step;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.LogbookLifecycleWorkerHelper;
import fr.gouv.vitam.worker.core.api.Worker;
import fr.gouv.vitam.worker.core.handler.AccessionRegisterActionHandler;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckDataObjectPackageActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckHeaderActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckIngestContractActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckNoObjectsActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckObjectUnitConsistencyActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckObjectsNumberActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckSedaActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckStorageAvailabilityActionHandler;
import fr.gouv.vitam.worker.core.handler.CheckVersionActionHandler;
import fr.gouv.vitam.worker.core.handler.CommitLifeCycleObjectGroupActionHandler;
import fr.gouv.vitam.worker.core.handler.CommitLifeCycleUnitActionHandler;
import fr.gouv.vitam.worker.core.handler.DummyHandler;
import fr.gouv.vitam.worker.core.handler.ExtractSedaActionHandler;
import fr.gouv.vitam.worker.core.handler.PrepareTraceabilityCheckProcessActionHandler;
import fr.gouv.vitam.worker.core.handler.RollBackActionHandler;
import fr.gouv.vitam.worker.core.handler.TransferNotificationActionHandler;
import fr.gouv.vitam.worker.core.handler.VerifyMerkleTreeActionHandler;
import fr.gouv.vitam.worker.core.handler.VerifyTimeStampActionHandler;
import fr.gouv.vitam.worker.core.plugin.PluginLoader;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;


/**
 * WorkerImpl class implements Worker interface
 * <p>
 * manages and executes actions by step
 */
public class WorkerImpl implements Worker {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerImpl.class);

    private static final String EMPTY_LIST = "null or Empty Action list";
    private static final String STEP_NULL = "step paramaters is null";
    private static final String HANDLER_NOT_FOUND = ": handler not found exception: ";
    private final Map<String, ActionHandler> actions = new HashMap<>();
    private final String workerId;
    private final PluginLoader pluginLoader;

    /**
     * Constructor
     *
     * @param pluginLoader the plugin loader
     */
    public WorkerImpl(PluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
        workerId = GUIDFactory.newGUID().toString();
        /*
         * temporary init: will be managed by spring annotation
         */
        init();
    }

    /**
     * Add an actionhandler in the pool of action
     *
     * @param actionName action name
     * @param actionHandler action handler
     * @return WorkerImpl
     */
    @Override
    public WorkerImpl addActionHandler(String actionName, ActionHandler actionHandler) {
        ParametersChecker.checkParameter("actionName is a mandatory parameter", actionName);
        ParametersChecker.checkParameter("actionHandler is a mandatory parameter", actionHandler);
        actions.put(actionName, actionHandler);
        return this;
    }

    private void init() {
        /*
         * Pool of action 's object
         */
        actions.put(ExtractSedaActionHandler.getId(), new ExtractSedaActionHandler());
        actions.put(CheckSedaActionHandler.getId(), new CheckSedaActionHandler());
        actions.put(CheckIngestContractActionHandler.getId(), new CheckIngestContractActionHandler());        
        actions.put(CheckObjectsNumberActionHandler.getId(), new CheckObjectsNumberActionHandler());
        actions.put(CheckNoObjectsActionHandler.getId(), new CheckNoObjectsActionHandler());
        actions.put(CheckVersionActionHandler.getId(), new CheckVersionActionHandler());
        actions.put(CheckStorageAvailabilityActionHandler.getId(),
            new CheckStorageAvailabilityActionHandler());
        actions.put(CheckObjectUnitConsistencyActionHandler.getId(),
            new CheckObjectUnitConsistencyActionHandler());
        actions.put(AccessionRegisterActionHandler.getId(),
            new AccessionRegisterActionHandler());
        actions.put(TransferNotificationActionHandler.getId(),
            new TransferNotificationActionHandler());
        actions.put(DummyHandler.getId(), new DummyHandler());

        actions.put(CommitLifeCycleUnitActionHandler.getId(),
            new CommitLifeCycleUnitActionHandler());
        actions.put(CommitLifeCycleObjectGroupActionHandler.getId(),
            new CommitLifeCycleObjectGroupActionHandler());

        actions.put(RollBackActionHandler.getId(),
            new RollBackActionHandler());

        actions.put(VerifyMerkleTreeActionHandler.getId(),
            new VerifyMerkleTreeActionHandler());

        actions.put(PrepareTraceabilityCheckProcessActionHandler.getId(),
            new PrepareTraceabilityCheckProcessActionHandler());

        actions.put(VerifyTimeStampActionHandler.getId(),
            new VerifyTimeStampActionHandler());
        actions.put(CheckHeaderActionHandler.getId(),
            new CheckHeaderActionHandler());
        actions.put(CheckDataObjectPackageActionHandler.getId(),
            new CheckDataObjectPackageActionHandler());

    }

    @Override
    public ItemStatus run(WorkerParameters workParams, Step step)
        throws IllegalArgumentException, ProcessingException, ContentAddressableStorageServerException {
        // mandatory check
        ParameterHelper.checkNullOrEmptyParameters(workParams);

        if (step == null) {
            throw new IllegalArgumentException(STEP_NULL);
        }

        if (step.getActions() == null || step.getActions().isEmpty()) {
            throw new IllegalArgumentException(EMPTY_LIST);
        }

        final ItemStatus responses = new ItemStatus(step.getStepName());

        try (final HandlerIO handlerIO = new HandlerIOImpl(workParams.getContainerName(), workerId);
            LogbookLifeCyclesClient logbookLfcClient = LogbookLifeCyclesClientFactory.getInstance().getClient()) {
            for (final Action action : step.getActions()) {
                // Reset handlerIO for next execution
                handlerIO.reset();
                ActionDefinition actionDefinition = action.getActionDefinition();
                if (actionDefinition.getIn() != null) {
                    handlerIO.addInIOParameters(actionDefinition.getIn());
                }
                if (actionDefinition.getOut() != null) {
                    handlerIO.addOutIOParameters(actionDefinition.getOut());
                }
                String handlerName = actionDefinition.getActionKey();
                ItemStatus actionResponse;
                // TODO Généralisation tous les workers
                // If this is a plugin
                if (pluginLoader.contains(handlerName)) {

                    try (ActionHandler actionPlugin = pluginLoader.newInstance(handlerName)) {
    
                        ItemStatus pluginResponse;
                        LOGGER.debug("START plugin ", actionDefinition.getActionKey(), step.getStepName());
                        boolean shouldWriteLFC = step.getDistribution().getKind().equals(DistributionKind.LIST);
                        if (shouldWriteLFC) {
                            LogbookLifeCycleParameters lfcParam = createStartLogbookLfc(step, handlerName, workParams);
                            logbookLfcClient.update(lfcParam);
                            pluginResponse = actionPlugin.execute(workParams, handlerIO);
                            writeLogBookLfcFromResponse(handlerName, logbookLfcClient, pluginResponse, lfcParam);
                        } else {
                            pluginResponse = actionPlugin.execute(workParams, handlerIO);
                        }
                        pluginResponse.setItemId(handlerName);
                        actionResponse = getActionResponse(handlerName, pluginResponse);
                    }
                    // If not, this is an handler of Vitam
                } else {
                    final ActionHandler actionHandler = getActionHandler(handlerName);
                    LOGGER.debug("START handler {} in step {}", actionDefinition.getActionKey(),
                        step.getStepName());
                    if (actionHandler == null) {
                        throw new HandlerNotFoundException(actionDefinition.getActionKey() + HANDLER_NOT_FOUND);
                    }
                    actionResponse = actionHandler.execute(workParams, handlerIO);
                }
                responses.setItemsStatus(actionResponse);
                LOGGER.debug("STOP handler {} in step {}", actionDefinition.getActionKey(),
                    step.getStepName());
                // if the action has been defined as Blocking and the action status is KO or FATAL
                // then break the process
                if (actionResponse.shallStop(ProcessBehavior.BLOCKING.equals(actionDefinition.getBehavior()))) {
                    break;
                }
            }


        } catch (Exception e) {
            throw new ProcessingException(e);
        }
        LOGGER.debug("step name :" + step.getStepName());
        return responses;
    }

    private ItemStatus getActionResponse(String handlerName, ItemStatus pluginResponse) {
        ItemStatus status = new ItemStatus(handlerName);
        for (final Entry<String, ItemStatus> entry : pluginResponse.getItemsStatus().entrySet()) {
            ItemStatus subItemStatus = entry.getValue();
            subItemStatus.setItemId(handlerName);
            status.setItemsStatus(handlerName, subItemStatus);
        }
        return status;
    }

    private LogbookLifeCycleParameters createStartLogbookLfc(Step step, String handlerName,
        WorkerParameters workParams) throws InvalidGuidOperationException {
        LogbookLifeCycleParameters lfcParam = null;
        if (step.getDistribution().getElement()
            .equals(LogbookType.UNITS.getType())) {
            lfcParam = LogbookParametersFactory.newLogbookLifeCycleUnitParameters(
                GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()),
                VitamLogbookMessages.getEventTypeLfc(handlerName),
                GUIDReader.getGUID(workParams.getContainerName()),
                // TODO Le type de process devrait venir du message recu (paramètre du workflow)
                workParams.getLogbookTypeProcess(),
                StatusCode.STARTED,
                VitamLogbookMessages.getOutcomeDetailLfc(handlerName, StatusCode.STARTED),
                VitamLogbookMessages.getCodeLfc(handlerName, StatusCode.STARTED),
                GUIDReader.getGUID(LogbookLifecycleWorkerHelper.getObjectID(workParams)));
        } else if (step.getDistribution().getElement()
            .equals(LogbookType.OBJECTGROUP.getType())) {
            lfcParam = LogbookParametersFactory.newLogbookLifeCycleObjectGroupParameters(
                GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter()),
                VitamLogbookMessages.getEventTypeLfc(handlerName),
                GUIDReader.getGUID(workParams.getContainerName()),
                // TODO Le type de process devrait venir du message recu (paramètre du workflow)
                workParams.getLogbookTypeProcess(),
                StatusCode.STARTED,
                VitamLogbookMessages.getOutcomeDetailLfc(handlerName, StatusCode.STARTED),
                VitamLogbookMessages.getCodeLfc(handlerName, StatusCode.STARTED),
                GUIDReader.getGUID(LogbookLifecycleWorkerHelper.getObjectID(workParams)));
        }
        return lfcParam;
    }

    private void writeLogBookLfcFromResponse(String handlerName, LogbookLifeCyclesClient logbookLfcClient,
        ItemStatus actionResponse, LogbookLifeCycleParameters logbookParam)
        throws LogbookClientBadRequestException, LogbookClientNotFoundException, LogbookClientServerException {

        List<LogbookLifeCycleParameters> logbookParamList = new ArrayList<>();
        LogbookLifeCycleParameters finalLogbookLfcParam = LogbookLifeCyclesClientHelper.copy(logbookParam);
        finalLogbookLfcParam.setFinalStatus(handlerName, null, actionResponse.getGlobalStatus(),
            actionResponse.getMessage());
        logbookParamList.add(finalLogbookLfcParam);
        for (final Entry<String, ItemStatus> entry : actionResponse.getItemsStatus().entrySet()) {
            for (final Entry<String, ItemStatus> subTaskEntry : entry.getValue().getSubTaskStatus().entrySet()) {
                LogbookLifeCycleParameters subLogbookLfcParam = LogbookLifeCyclesClientHelper.copy(logbookParam);
                ItemStatus subItemStatus = subTaskEntry.getValue();
                subLogbookLfcParam.setFinalStatus(handlerName,
                    entry.getKey(), subItemStatus.getGlobalStatus(), subItemStatus.getMessage());
                if (!subItemStatus.getEvDetailData().isEmpty()) {
                    subLogbookLfcParam.putParameterValue(LogbookParameterName.eventDetailData,
                        subItemStatus.getEvDetailData());
                }
                logbookParamList.add(subLogbookLfcParam);
            }
            entry.getValue().getSubTaskStatus().clear();
        }
        for (int i = logbookParamList.size() - 1; i >= 0; i--) {
            logbookLfcClient.update(logbookParamList.get(i));
        }
    }

    private ActionHandler getActionHandler(String actionId) throws PluginNotFoundException {
        return actions.get(actionId);
    }

    @Override
    public String getWorkerId() {
        return workerId;
    }

    @Override
    public void close() {
        actions.clear();
    }
}
