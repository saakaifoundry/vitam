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
package fr.gouv.vitam.worker.core.plugin;

import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.common.utils.SedaConstants;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

/**
 * 
 */
public abstract class StoreObjectActionHandler extends ActionHandler {


    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(StoreObjectActionHandler.class);

    private static final String DEFAULT_STRATEGY = "default";

    private final StorageClientFactory storageClientFactory = StorageClientFactory.getInstance();

    /**
     * The function is used for retrieving ObjectGroup in workspace and storing metaData in storage offer
     *
     * @param description the object description
     * @param itemStatus item status
     * @return StoredInfoResult
     * @throws ProcessingException when error in execution
     */
    protected StoredInfoResult storeObject(ObjectDescription description,
        ItemStatus itemStatus) throws ProcessingException {

        try (final StorageClient storageClient = storageClientFactory.getClient()) {
            // store binary data object
            return storageClient.storeFileFromWorkspace(DEFAULT_STRATEGY, description.getType(),
                description.getObjectName(),
                description);

        } catch (StorageAlreadyExistsClientException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.KO);
        } catch (StorageNotFoundClientException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (StorageServerClientException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return null;
    }

    /**
     * Helper to set _storage on node after storing it
     * 
     * @param node
     * @param result
     * @param update True will return an UpdateMultiQuery, else null
     * 
     * @throws InvalidCreateOperationException 
     */
    protected UpdateMultiQuery storeStorageInfo(ObjectNode node, StoredInfoResult result, boolean update) throws InvalidCreateOperationException {
        LOGGER.debug("DEBUG result: {}", result);
        int nbc = result.getNbCopy();
        String strategy = result.getStrategy();
        List<String> offers = result.getOfferIds();
        ObjectNode storage = JsonHandler.createObjectNode();
        storage.put(SedaConstants.TAG_NB, nbc);
        ArrayNode offersId = JsonHandler.createArrayNode();
        if (offers != null) {
            for (String id : offers) {
                offersId.add(id);
            }
        }
        storage.set(SedaConstants.OFFER_IDS, offersId);
        storage.put(SedaConstants.STRATEGY_ID, strategy);
        node.set(SedaConstants.STORAGE, storage);
        LOGGER.debug("DEBUG node: {}", node);
        if (update) {
            UpdateMultiQuery query = new UpdateMultiQuery();
            query.addActions(UpdateActionHelper.set(VitamFieldsHelper.storage() + "." + SedaConstants.TAG_NB, nbc)
                .add(VitamFieldsHelper.storage() + "." + SedaConstants.STRATEGY_ID, strategy)
                .add(VitamFieldsHelper.storage() + "." + SedaConstants.OFFER_IDS, offers));
            LOGGER.debug("DEBUG query: {}", query);
            return query;
        }
        return null;
    }
}
