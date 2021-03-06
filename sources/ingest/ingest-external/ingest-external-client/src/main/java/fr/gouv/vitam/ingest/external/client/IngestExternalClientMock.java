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
package fr.gouv.vitam.ingest.external.client;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.client.IngestCollection;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.ProcessQuery;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;

/**
 * Mock client implementation for IngestExternal
 */
class IngestExternalClientMock extends AbstractMockClient implements IngestExternalClient {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestExternalClientMock.class);
    private static final String FAKE_X_REQUEST_ID = GUIDFactory.newRequestIdGUID(0).getId();
    public static final String MOCK_INGEST_EXTERNAL_RESPONSE_STREAM = "VITAM-Ingest External Client Mock Response";
    private static final String FAKE_EXECUTION_STATUS = "Fake";
    final int TENANT_ID = 0;
    public static final String ID = "identifier1";
    protected StatusCode globalStatus;

    @Override
    public RequestResponse<JsonNode> upload(InputStream stream, Integer tenantId, String contextId, String action)
        throws IngestExternalException {
        if (stream == null) {
            throw new IngestExternalException("stream is null");
        }
        StreamUtils.closeSilently(stream);

        RequestResponseOK r = new RequestResponseOK<JsonNode>();
        r.setHttpCode(Status.ACCEPTED.getStatusCode());
        r.addHeader(FAKE_X_REQUEST_ID, X_REQUEST_ID);

        return r;
    }

    /**
     * Generate the default header map
     *
     * @param requestId fake x-request-id
     * @return header map
     */
    private MultivaluedHashMap<String, Object> getDefaultHeaders(String requestId, Integer tenantId) {
        final MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(X_REQUEST_ID, requestId);
        headers.add(GlobalDataRest.X_TENANT_ID, tenantId);
        return headers;
    }

    @Override
    public Response downloadObjectAsync(String objectId, IngestCollection type, Integer tenantId)
        throws IngestExternalException {
        return ClientMockResultHelper.getObjectStream();
    }

    @Override
    public ItemStatus getOperationProcessStatus(String id, Integer tenantId)
        throws VitamClientException {
        ItemStatus pwork = null;
        try {
            pwork = ClientMockResultHelper.getItemStatus(id);
        } catch (InvalidParseOperationException e) {
            LOGGER.error(e);
            throw new VitamClientException(e.getMessage(), e);
        }
        return pwork;
    }
    // TODO FIXE ME query never user

    @Override
    public ItemStatus getOperationProcessExecutionDetails(String id, JsonNode query, Integer tenantId)
        throws VitamClientException {
        return new ItemStatus(ID);
    }

    @Override
    public RequestResponse<JsonNode> cancelOperationProcessExecution(String id, Integer tenantId)
        throws VitamClientException {
        // return new ItemStatus(ID);

        return new RequestResponseOK().setHttpCode(Status.OK.getStatusCode());
    }

    @Override
    public Response updateOperationActionProcess(String actionId, String id, Integer tenantId)
        throws VitamClientException {
        return Response.status(Status.OK).build();
    }

    @Override
    public RequestResponse<JsonNode> executeOperationProcess(String operationId, String workflow, String contextId,
        String actionId, Integer tenantId)
        throws VitamClientException {
        return new RequestResponseOK<JsonNode>().addHeader(GlobalDataRest.X_GLOBAL_EXECUTION_STATE,
            FAKE_EXECUTION_STATUS);
    }

    @Override
    public void initWorkFlow(String contextId, Integer tenantId) throws VitamException {}

    @Override
    public ItemStatus updateVitamProcess(String contextId, String actionId, String container, String workflow,
        Integer tenantId)
        throws VitamClientException {
        return new ItemStatus(ID);
    }

    @Override
    public void initVitamProcess(String contextId, String container, String workflow, Integer tenantId)
        throws VitamClientException {}

    @Override
    public RequestResponse<JsonNode> listOperationsDetails(Integer tenantId, ProcessQuery query)
        throws VitamClientException {
        return RequestResponse.parseFromResponse(Response.status(Status.OK).build());
    }

    @Override
    public RequestResponse<JsonNode> getWorkflowDefinitions(Integer tenantId) throws VitamClientException {
        return RequestResponse.parseFromResponse(Response.status(Status.OK).build());
    }

    @Override
    public boolean wait(int tenantId, String processId, ProcessState state, int nbTry, long timeout, TimeUnit timeUnit)
        throws VitamException {
        return true;
    }

    @Override
    public boolean wait(int tenantId, String processId, int nbTry, long timeout, TimeUnit timeUnit)
        throws VitamException {
        return true;
    }

    @Override
    public boolean wait(int tenantId, String processId, ProcessState state) throws VitamException {
        return true;
    }

    @Override
    public boolean wait(int tenantId, String processId) throws VitamException {
        return true;
    }
}
