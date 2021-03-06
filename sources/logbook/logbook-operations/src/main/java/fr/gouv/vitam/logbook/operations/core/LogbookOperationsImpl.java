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
package fr.gouv.vitam.logbook.operations.core;

import static fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName.outcomeDetail;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.common.server.LogbookDbAccess;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.server.exception.LogbookAlreadyExistsException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookDatabaseException;
import fr.gouv.vitam.logbook.common.server.exception.LogbookNotFoundException;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;

/**
 * Logbook Operations implementation base class
 */
public class LogbookOperationsImpl implements LogbookOperations {
    private final LogbookDbAccess mongoDbAccess;

    /**
     * Constructor
     *
     * @param mongoDbAccess of logbook
     */
    public LogbookOperationsImpl(LogbookDbAccess mongoDbAccess) {
        this.mongoDbAccess = mongoDbAccess;
    }

    @Override
    public void create(LogbookOperationParameters parameters) throws LogbookAlreadyExistsException,
        LogbookDatabaseException {
        mongoDbAccess.createLogbookOperation(parameters);
    }

    @Override
    public void update(LogbookOperationParameters parameters)
        throws LogbookNotFoundException, LogbookDatabaseException {
        mongoDbAccess.updateLogbookOperation(parameters);
    }

    @Override
    public List<LogbookOperation> select(JsonNode select)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException {
        return select(select, true);
    }

    @Override
    public List<LogbookOperation> select(JsonNode select, boolean sliced)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidParseOperationException {
        try (final MongoCursor<LogbookOperation> logbook = mongoDbAccess.getLogbookOperations(select, sliced)) {
            final List<LogbookOperation> result = new ArrayList<>();
            if (logbook == null || !logbook.hasNext()) {
                throw new LogbookNotFoundException("Logbook entry not found");
            }
            while (logbook.hasNext()) {
                result.add(logbook.next());
            }
            return result;
        }
    }

    @Override
    public LogbookOperation getById(String idProcess) throws LogbookDatabaseException, LogbookNotFoundException {
        return mongoDbAccess.getLogbookOperation(idProcess);
    }

    @Override
    public final void createBulkLogbookOperation(final LogbookOperationParameters[] operationArray)
        throws LogbookDatabaseException, LogbookAlreadyExistsException {
        mongoDbAccess.createBulkLogbookOperation(operationArray);
    }

    @Override
    public final void updateBulkLogbookOperation(final LogbookOperationParameters[] operationArray)
        throws LogbookDatabaseException, LogbookNotFoundException {
        mongoDbAccess.updateBulkLogbookOperation(operationArray);
    }

    @Override
    public MongoCursor<LogbookOperation> selectAfterDate(final LocalDateTime date)
        throws LogbookDatabaseException, LogbookNotFoundException, InvalidCreateOperationException,
        InvalidParseOperationException {
        final Select select = logbookOperationsAfterDateQuery(date);
        return mongoDbAccess.getLogbookOperations(select.getFinalSelect(), false);

    }

    private Select logbookOperationsAfterDateQuery(final LocalDateTime date)
        throws InvalidCreateOperationException, InvalidParseOperationException {

        final Query parentQuery = QueryHelper.gte("evDateTime", date.toString());
        final Query sonQuery = QueryHelper.gte(LogbookDocument.EVENTS + ".evDateTime", date.toString());
        final Select select = new Select();
        select.setQuery(QueryHelper.or().add(parentQuery, sonQuery));
        select.addOrderByAscFilter("evDateTime");
        return select;
    }

    @Override
    public LogbookOperation findFirstTraceabilityOperationOKAfterDate(final LocalDateTime date)
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException {
        final Select select = new Select();
        final Query query = QueryHelper.gt("evDateTime", date.toString());
        final Query type = QueryHelper.eq("evTypeProc", LogbookTypeProcess.TRACEABILITY.name());
        final Query status =
            QueryHelper.eq(LogbookDocument.EVENTS + "." + outcomeDetail.getDbname(), "STP_OP_SECURISATION.OK");
        select.setQuery(QueryHelper.and().add(query, type, status));
        select.setLimitFilter(0, 1);
        return Iterators.getOnlyElement(mongoDbAccess.getLogbookOperations(select.getFinalSelect(), false), null);
    }

    @Override
    public LogbookOperation findLastTraceabilityOperationOK()
        throws InvalidCreateOperationException, LogbookNotFoundException, LogbookDatabaseException,
        InvalidParseOperationException {
        final Select select = new Select();
        final Query type = QueryHelper.eq("evTypeProc", LogbookTypeProcess.TRACEABILITY.name());
        final Query findEvent = QueryHelper
            .eq(String.format("%s.%s", LogbookDocument.EVENTS, outcomeDetail.getDbname()), "STP_OP_SECURISATION.OK");

        select.setLimitFilter(0, 1);
        select.setQuery(QueryHelper.and().add(type, findEvent));

        select.addOrderByDescFilter("evDateTime");

        return Iterators.getOnlyElement(mongoDbAccess.getLogbookOperations(select.getFinalSelect(), false), null);
    }
}
