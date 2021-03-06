/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.access.internal.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.access.internal.api.AccessInternalResource;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientNotFoundException;
import fr.gouv.vitam.access.internal.common.exception.AccessInternalClientServerException;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;

public class AccessInternalClientRestTest extends VitamJerseyTest {
    protected static final String HOSTNAME = "localhost";
    protected static final int PORT = 8082;
    protected static final String PATH = "/access/v1";
    private static final String DUMMY_REQUEST_ID = "reqId";
    protected AccessInternalClientRest client;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    final String queryDsql =
        "{ \"$query\" : [ { \"$eq\": { \"title\" : \"test\" } } ], " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";
    final String emptyQueryDsql =
        "{ \"$query\" : \"\", " +
            " \"$filter\": { \"$orderby\": \"#id\" }, " +
            " \"$projection\" : { \"$fields\" : { \"#id\": 1, \"title\" : 2, \"transacdate\": 1 } } " +
            " }";
    final String ID = "identfier1";
    final String USAGE = "BinaryMaster";
    final int VERSION = 1;

    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    public AccessInternalClientRestTest() {
        super(AccessInternalClientFactory.getInstance());
    }

    @Override
    public void beforeTest() {
        client = (AccessInternalClientRest) getClient();
    }

    // Define the getApplication to return your Application using the correct Configuration
    @Override
    public StartApplicationResponse<AbstractApplication> startVitamApplication(int reservedPort) {
        final TestVitamApplicationConfiguration configuration = new TestVitamApplicationConfiguration();
        configuration.setJettyConfig(DEFAULT_XML_CONFIGURATION_FILE);
        final AbstractApplication application = new AbstractApplication(configuration);
        try {
            application.start();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException("Cannot start the application", e);
        }
        return new StartApplicationResponse<AbstractApplication>()
            .setServerPort(application.getVitamServer().getPort())
            .setApplication(application);
    }

    // Define your Application class if necessary
    public final class AbstractApplication
        extends AbstractVitamApplication<AbstractApplication, TestVitamApplicationConfiguration> {
        protected AbstractApplication(TestVitamApplicationConfiguration configuration) {
            super(TestVitamApplicationConfiguration.class, configuration);
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            resourceConfig.registerInstances(new MockResource(mock));
        }

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }

    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

    }

    @Path("/access-internal/v1")
    @javax.ws.rs.ApplicationPath("webresources")
    public static class MockResource extends ApplicationStatusResource implements AccessInternalResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @Override
        @GET
        @Path("/units")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnits(JsonNode queryDsl) {
            return expectedResponse.post();
        }

        @Override
        @GET
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getUnitById(JsonNode queryDsl,
            @PathParam("id_unit") String id_unit) {
            return expectedResponse.post();
        }

        @Override
        @PUT
        @Path("/units/{id_unit}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response updateUnitById(JsonNode queryDsl, @PathParam("id_unit") String id_unit,
            @HeaderParam(GlobalDataRest.X_REQUEST_ID) String requestId) {
            return expectedResponse.put();
        }

        @GET
        @Path("/status")
        @Override
        public Response status() {
            return expectedResponse.get();
        }

        @Override
        @GET
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response getObjectGroup(@PathParam("id_object_group") String idObjectGroup, JsonNode query) {
            return expectedResponse.get();
        }

        @Override
        @GET
        @Path("/objects/{id_object_group}")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public void getObjectStreamAsync(@Context HttpHeaders headers,
            @PathParam("id_object_group") String idObjectGroup,
            JsonNode query, @Suspended final AsyncResponse asyncResponse) {
            asyncResponse.resume(expectedResponse.get());
        }

        // Functionalities related to TRACEABILITY operation

        @POST
        @Path("/traceability/check")
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Response checkTraceabilityOperation(JsonNode query)
            throws InvalidParseOperationException {
            return expectedResponse.post();
        }

        @GET
        @Path("/traceability/{idOperation}/content")
        @Produces(MediaType.APPLICATION_OCTET_STREAM)
        public Response downloadTraceabilityOperationFile(@PathParam("idOperation") String operationId)
            throws InvalidParseOperationException {
            return expectedResponse.get();
        }

    }

    @RunWithCustomExecutor
    @Test(expected = AccessInternalClientServerException.class)
    public void givenInternalServerError_whenSelect_ThenRaiseAnExeption() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.selectUnits(queryJson)).isNotNull();
    }
    
    @RunWithCustomExecutor
    @Test(expected = BadRequestException.class)
    public void givenBadRequestException_whenSelect_ThenRaiseAnExeption() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.FORBIDDEN).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(emptyQueryDsql);
        client.selectUnits(queryJson);
    }

    @RunWithCustomExecutor
    @Test(expected = AccessInternalClientNotFoundException.class)
    public void givenRessourceNotFound_whenSelectUnit_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.selectUnits(queryJson)).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenSelectUnit_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.selectUnits(queryJson)).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenBlankRequest_whenSelectUnit_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString("");
        assertThat(client.selectUnits(queryJson)).isNotNull();

    }
    // Select Unit By Id


    @RunWithCustomExecutor
    @Test(expected = AccessInternalClientServerException.class)
    public void givenInternalServerError_whenSelectById_ThenRaiseAnExeption() throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.selectUnitbyId(queryJson, ID)).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = AccessInternalClientNotFoundException.class)
    public void givenResourceNotFound_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.NOT_FOUND).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.selectUnitbyId(queryJson, ID)).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        when(mock.post()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        client.selectUnitbyId(queryJson, ID);
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenBlankRequest_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString("");
        assertThat(client.selectUnitbyId(queryJson, "")).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void givenBlankID_whenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.selectUnitbyId(queryJson, "")).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenBlankRequest_IDFilledwhenSelectUnitById_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString("");
        assertThat(client.selectUnitbyId(queryJson, ID)).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenUpdateUnitById_ThenRaiseAnException()
        throws Exception {
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.updateUnitbyId(queryJson, ID)).isNotNull();
    }


    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenBlankRequest_whenUpdateUnitById_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString("");
        assertThat(client.updateUnitbyId(queryJson, "")).isNotNull();
    }


    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void givenIdBlank_whenUpdateUnitById_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.updateUnitbyId(queryJson, "")).isNotNull();
    }


    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenBlankRequest_IDFilledWhenUpdateUnitById_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        final JsonNode queryJson = JsonHandler.getFromString("");
        assertThat(client.updateUnitbyId(queryJson, ID)).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenBadRequest_whenUpdateUnit_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.put()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.updateUnitbyId(queryJson, ID)).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = AccessInternalClientServerException.class)
    public void given500_whenUpdateUnit_ThenRaiseAnException()
        throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.put()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.updateUnitbyId(queryJson, ID)).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void givenQueryNullWhenSelectObjectByIdThenRaiseAnIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        client.selectObjectbyId(null, ID);
    }

    @RunWithCustomExecutor
    @Test(expected = AccessInternalClientServerException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        client.selectObjectbyId(queryJson, ID);
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenRaiseBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        client.selectObjectbyId(queryJson, ID);
    }

    @RunWithCustomExecutor
    @Test(expected = AccessInternalClientServerException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenRaisePreconditionFailed() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        client.selectObjectbyId(queryJson, ID);
    }

    @RunWithCustomExecutor
    @Test(expected = AccessInternalClientNotFoundException.class)
    public void givenQueryCorrectWhenSelectObjectByIdThenNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        client.selectObjectbyId(queryJson, ID);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenSelectObjectByIdThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK).entity("{ \"hint\": {\"total\":\"1\"} }").build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        assertThat(client.selectObjectbyId(queryJson, ID)).isNotNull();
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void givenQueryNullWhenGetObjectAsInputStreamThenRaiseAnIllegalArgumentException() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        client.getObject(null, ID, USAGE, VERSION);
    }

    @RunWithCustomExecutor
    @Test(expected = AccessInternalClientServerException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaiseInternalServerError() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.INTERNAL_SERVER_ERROR).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        client.getObject(queryJson, ID, USAGE, VERSION);
    }

    @RunWithCustomExecutor
    @Test(expected = InvalidParseOperationException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaiseBadRequest() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        client.getObject(queryJson, ID, USAGE, VERSION);
    }

    @RunWithCustomExecutor
    @Test(expected = IllegalArgumentException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenRaisePreconditionFailed() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.PRECONDITION_FAILED).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        client.getObject(queryJson, ID, USAGE, VERSION);
    }

    @RunWithCustomExecutor
    @Test(expected = AccessInternalClientNotFoundException.class)
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenNotFound() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.NOT_FOUND).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        client.getObject(queryJson, ID, USAGE, VERSION);
    }

    @RunWithCustomExecutor
    @Test
    public void givenQueryCorrectWhenGetObjectAsInputStreamThenOK() throws Exception {
        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(Response.status(Status.OK).entity(IOUtils.toInputStream("Vitam test")).build());
        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        final InputStream stream = client.getObject(queryJson, ID, USAGE, VERSION).readEntity(InputStream.class);
        final InputStream stream2 = IOUtils.toInputStream("Vitam test");
        assertNotNull(stream);
        assertTrue(IOUtils.contentEquals(stream, stream2));
    }

    @Test
    public void statusExecutionWithouthBody() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.OK).build());
        client.checkStatus();
    }

    @RunWithCustomExecutor
    @Test
    public void givenCorrectDslQueryWhenCheckTraceabilityOperationThenOK() throws Exception {

        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.post()).thenReturn(Response.ok().entity(ClientMockResultHelper.checkOperationTraceability()).build());

        final JsonNode queryJson = JsonHandler.getFromString(queryDsql);
        @SuppressWarnings("rawtypes")
        final RequestResponse requestResponse = client.checkTraceabilityOperation(queryJson);
        assertNotNull(requestResponse);
        assertTrue(requestResponse.toJsonNode().has("$results"));
    }

    @RunWithCustomExecutor
    @Test
    public void givenOperationIdWhenDownloadTraceabilityOperationThenOK() throws Exception {

        VitamThreadUtils.getVitamSession().setRequestId(DUMMY_REQUEST_ID);
        when(mock.get()).thenReturn(ClientMockResultHelper.getObjectStream());

        Response response = client.downloadTraceabilityFile("OP_ID");
        assertNotNull(response);
    }

}
