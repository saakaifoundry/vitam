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
package fr.gouv.vitam.logbook.common.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleObjectGroupParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookLifeCycleUnitParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookCollections;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;

/**
 * MongoDbAccess interface
 */
public interface LogbookDbAccess {

    /**
     * Close database access
     */
    void close();

    /**
     *
     * @return the current number of Logbook Operation
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    long getLogbookOperationSize() throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     *
     * @return the current number of Logbook LifeCyle
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    long getLogbookLifeCyleUnitSize() throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     *
     * @return the current number of Logbook LifeCyle
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    long getLogbookLifeCyleObjectGroupSize() throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Check if one eventIdentifier for Operation exists already
     *
     * @param operationItem
     * @return True if one LogbookOperation exists with this id
     *
     * @throws LogbookDatabaseException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    boolean existsLogbookOperation(final String operationItem) throws LogbookDatabaseException;

    /**
     * Check if one eventIdentifier for Lifecycle exists already
     *
     * @param lifecycleItem
     * @return True if one LogbookLibeCycle exists with this id
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    boolean existsLogbookLifeCycleUnit(final String lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Check if one eventIdentifier for Lifecycle exists already
     *
     * @param lifecycleItem
     * @return True if one LogbookLibeCycle exists with this id
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    boolean existsLogbookLifeCycleObjectGroup(final String lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get one Operation
     *
     * @param eventIdentifierProcess
     * @return the corresponding LogbookOperation if it exists
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    LogbookOperation getLogbookOperation(final String eventIdentifierProcess)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get one Lifecycle
     *
     * @param objectIdentifier
     * @return the corresponding LogbookLibeCycle if it exists
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    LogbookLifeCycleUnit getLogbookLifeCycleUnit(final String objectIdentifier)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get one Lifecycle
     *
     * @param objectIdentifier
     * @return the full corresponding LogbookLibeCycle if it exists
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    LogbookLifeCycleObjectGroup getLogbookLifeCycleObjectGroup(final String objectIdentifier)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get one Lifecycle
     *
     * @param idOperation
     * @param idLc
     * @return the full corresponding LogbookLibeCycle if it exists and linked to the given operation
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    LogbookLifeCycleUnit getLogbookLifeCycleUnit(final String idOperation, final String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get one Lifecycle
     *
     * @param idOperation
     * @param idLc
     * @return the corresponding LogbookLibeCycle if it exists and linked to the given operation
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    LogbookLifeCycleObjectGroup getLogbookLifeCycleObjectGroup(final String idOperation, final String idLc)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Create one Logbook Operation
     *
     * @param operationItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void createLogbookOperation(final LogbookOperationParameters operationItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Create one Logbook LifeCycle unit
     *
     * @param idOperation
     * @param lifecycleItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void createLogbookLifeCycleUnit(final String idOperation, final LogbookLifeCycleUnitParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Create one Logbook LifeCycle object group
     *
     * @param idOperation
     * @param lifecycleItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void createLogbookLifeCycleObjectGroup(final String idOperation,
        final LogbookLifeCycleObjectGroupParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookAlreadyExistsException;


    /**
     * Update one Logbook Operation <br>
     * <br>
     * It adds this new entry within the very same Logbook Operaton entry in "events" array.
     *
     * @param operationItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void updateLogbookOperation(LogbookOperationParameters operationItem)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Update one Logbook LifeCycle <br>
     * <br>
     * It adds this new entry within the very same Logbook LifeCycle entry in "events" array.
     *
     * @param idOperation
     * @param lifecycleItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void updateLogbookLifeCycleUnit(final String idOperation, LogbookLifeCycleUnitParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException;


    /**
     * Update one Logbook LifeCycle <br>
     * <br>
     * It adds this new entry within the very same Logbook LifeCycle entry in "events" array.
     *
     * @param idOperation
     * @param lifecycleItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void updateLogbookLifeCycleObjectGroup(final String idOperation,
        LogbookLifeCycleObjectGroupParameters lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Rollback one Logbook LifeCycle <br>
     * <br>
     * It adds this new entry within the very same Logbook LifeCycle entry in "events" array.
     *
     * @param idOperation
     * @param lifecycleItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void rollbackLogbookLifeCycleUnit(final String idOperation, String lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Rollback one Logbook LifeCycle <br>
     * <br>
     * It adds this new entry within the very same Logbook LifeCycle entry in "events" array.
     *
     * @param idOperation
     * @param lifecycleItem
     *
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     */
    void rollbackLogbookLifeCycleObjectGroup(final String idOperation, String lifecycleItem)
        throws LogbookDatabaseException, LogbookNotFoundException;


    /**
     * Create one Logbook Operation with already multiple sub-events
     *
     * @param operationItems with first and next events to add/update
     *
     * @throws IllegalArgumentException if first argument is null or null mandatory parameters for all
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     */
    void createBulkLogbookOperation(LogbookOperationParameters... operationItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Create one Logbook LifeCycle with already multiple sub-events
     *
     * @param lifecycleItems with first and next events to add/update
     *
     * @throws IllegalArgumentException if first argument is null or null mandatory parameters for all
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     */
    void createBulkLogbookLifeCycleUnit(LogbookLifeCycleUnitParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Create one Logbook LifeCycle with already multiple sub-events
     *
     * @param lifecycleItems with first and next events to add/update
     *
     * @throws IllegalArgumentException if first argument is null or null mandatory parameters for all
     * @throws LogbookDatabaseException
     * @throws LogbookAlreadyExistsException
     */
    void createBulkLogbookLifeCycleObjectGroup(LogbookLifeCycleObjectGroupParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookAlreadyExistsException;

    /**
     * Update one Logbook Operation with multiple sub-events <br>
     * <br>
     * It adds this new entry within the very same Logbook Operaton entry in "events" array.
     *
     * @param operationItems
     *
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    void updateBulkLogbookOperation(LogbookOperationParameters... operationItems)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Update one Logbook LifeCycle with multiple sub-events <br>
     * <br>
     * It adds this new entry within the very same Logbook LifeCycle entry in "events" array.
     *
     * @param lifecycleItems
     *
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    void updateBulkLogbookLifeCycleUnit(LogbookLifeCycleUnitParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Update one Logbook LifeCycle with multiple sub-events <br>
     * <br>
     * It adds this new entry within the very same Logbook LifeCycle entry in "events" array.
     *
     * @param lifecycleItems
     *
     * @throws IllegalArgumentException if parameter has null or empty mandatory values
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    void updateBulkLogbookLifeCycleObjectGroup(LogbookLifeCycleObjectGroupParameters... lifecycleItems)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get a list of Logbook Operation through Closeable MongoCursor
     *
     * @param select
     * @return the Closeable MongoCursor of LogbookOperation
     *
     * @throws IllegalArgumentException if argument is null or empty
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    MongoCursor<LogbookOperation> getLogbookOperations(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get a list of Logbook LifeCycle through Closeable MongoCursor
     *
     * @param select
     * @return the Closeable MongoCursor of LogbookLifeCycle
     *
     * @throws IllegalArgumentException if argument is null or empty
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    MongoCursor<LogbookLifeCycleUnit> getLogbookLifeCycleUnits(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException;

    /**
     * Get a list of Logbook LifeCycle through Closeable MongoCursor
     *
     * @param select
     * @return the Closeable MongoCursor of LogbookLifeCycle
     *
     * @throws IllegalArgumentException if argument is null or empty
     * @throws LogbookDatabaseException
     */
    MongoCursor<LogbookLifeCycleUnit> getLogbookLifeCycleUnitsFull(Select select)
        throws LogbookDatabaseException;

    /**
     * Get a list of Logbook LifeCycle through Closeable MongoCursor
     *
     * @param select
     * @return the Closeable MongoCursor of LogbookLifeCycle
     *
     * @throws IllegalArgumentException if argument is null or empty
     * @throws LogbookDatabaseException
     * @throws LogbookNotFoundException
     */
    MongoCursor<LogbookLifeCycleObjectGroup> getLogbookLifeCycleObjectGroups(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException;


    /**
     * Get a list of Logbook LifeCycle through Closeable MongoCursor
     *
     * @param select
     * @return the Closeable MongoCursor of LogbookLifeCycle
     *
     * @throws IllegalArgumentException if argument is null or empty
     * @throws LogbookDatabaseException
     */
    MongoCursor<LogbookLifeCycleObjectGroup> getLogbookLifeCycleObjectGroupsFull(Select select)
        throws LogbookDatabaseException;

    /**
     * Delete logbook collection
     *
     * @param collection the logbook collection to delete
     * @throws DatabaseException thrown when error on delete
     */
    void deleteCollection(LogbookCollections collection) throws DatabaseException;
}
