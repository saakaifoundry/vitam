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
package fr.gouv.vitam.common.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.servlet.DispatcherType;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.VitamConfiguration;
import org.apache.shiro.web.env.EnvironmentLoaderListener;
import org.apache.shiro.web.servlet.ShiroFilter;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.configuration.SSLConfiguration;
import fr.gouv.vitam.common.client.configuration.SSLKey;
import fr.gouv.vitam.common.client.configuration.SecureClientConfiguration;
import fr.gouv.vitam.common.client.configuration.SecureClientConfigurationImpl;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.junit.VitamApplicationTestFactory.StartApplicationResponse;
import fr.gouv.vitam.common.logging.SysErrLogger;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.junit.MinimalTestVitamApplicationFactory;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest.ExpectedResults;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.server.benchmark.BenchmarkConfiguration;

public class VitamRequestIteratorSslTest {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRequestIteratorSslTest.class);


    private static final String BASE_URI = "/ingest-ext/v1";
    private static final String INGEST_EXTERNAL_CONF = "standard-application-ssl-test.conf";
    private static final String SHIRO_FILE = "shiro.ini";
    private static final String INGEST_EXTERNAL_CLIENT_CONF = "standard-client-secure.conf";
    private static TestVitamApplication application;
    private static int serverPort;

    private static boolean startup = true;
    private static final ExpectedResults mock = mock(ExpectedResults.class);
    private static VitamClientFactory<DefaultClient> factory;
    
    @Path(BASE_URI)
    @javax.ws.rs.ApplicationPath("webresources")
    public static class SslResource extends ApplicationStatusResource {
        private final ExpectedResults expectedResponse;

        public SslResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @GET
        @Path("/iterator")
        @Produces(MediaType.APPLICATION_JSON)
        public Response iterator(@Context HttpHeaders headers) {
            final Response response = expectedResponse.get();
            final boolean checkStart = VitamRequestIterator.isNewCursor(headers);
            VitamRequestIterator.isEndOfCursor(headers);
            assertEquals(startup, checkStart);
            startup = false;
            return response;
        }
    }

    private static class TestVitamApplication
        extends AbstractVitamApplication<TestVitamApplication, BenchmarkConfiguration> {

        protected TestVitamApplication(String config) {
            super(BenchmarkConfiguration.class, config);
        }

        protected TestVitamApplication(BenchmarkConfiguration config) {
            super(BenchmarkConfiguration.class, config);
        }

        @Override
        protected void platformSecretConfiguration() {
            VitamConfiguration.setFilterActivation(false);

            // Nothing
        }

        @Override
        protected void checkJerseyMetrics(ResourceConfig resourceConfig) {
            // Nothing
        }

        @Override
        protected void setFilter(ServletContextHandler context) throws VitamApplicationServerException {
            File shiroFile = null;
            try {
                shiroFile = PropertiesUtils.findFile(SHIRO_FILE);
            } catch (final FileNotFoundException e) {
                throw new VitamApplicationServerException(e.getMessage());
            }
            LOGGER.info("Start Shiro configuration");
            context.setInitParameter("shiroConfigLocations", "file:" + shiroFile.getAbsolutePath());
            context.addEventListener(new EnvironmentLoaderListener());
            context.addFilter(ShiroFilter.class, "/*", EnumSet.of(
                DispatcherType.INCLUDE, DispatcherType.REQUEST,
                DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.ASYNC));
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            resourceConfig.register(new SslResource(mock));
        }

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
       final MinimalTestVitamApplicationFactory<TestVitamApplication> testFactory =
            new MinimalTestVitamApplicationFactory<TestVitamApplication>() {

                @Override
                public StartApplicationResponse<TestVitamApplication> startVitamApplication(int reservedPort)
                    throws IllegalStateException {
                    final TestVitamApplication application = new TestVitamApplication(INGEST_EXTERNAL_CONF);
                    final StartApplicationResponse<TestVitamApplication> response = startAndReturn(application);
                    return response;
                }

            };
        final StartApplicationResponse<TestVitamApplication> response = testFactory.findAvailablePortSetToApplication();
        serverPort = response.getServerPort();
        application = response.getApplication();
        LOGGER.warn("Start configuration: " + serverPort);
        final SecureClientConfiguration configuration = changeConfigurationFile(INGEST_EXTERNAL_CLIENT_CONF);
        configuration.setServerPort(serverPort);

        factory =
            new VitamClientFactory<DefaultClient>(configuration, BASE_URI) {

                @Override
                public DefaultClient getClient() {
                    return new DefaultClient(this);
                }

            };
        factory.disableUseAuthorizationFilter();
        LOGGER.warn("Start ClientFactory: " + factory);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        try {
            if (application != null) {
                application.stop();
            }
        } catch (final VitamApplicationServerException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        JunitHelper.getInstance().releasePort(serverPort);
    }

    @Test
    public void testClientBuilder() throws Exception {
        final SSLKey key = new SSLKey("tls/client/client.p12", "vitam2016");
        final ArrayList<SSLKey> truststore = new ArrayList<>();
        truststore.add(key);
        final SSLConfiguration sslConfig = new SSLConfiguration(truststore, truststore);
        final SecureClientConfiguration configuration =
            new SecureClientConfigurationImpl("host", 8443, true, sslConfig, false);
        final VitamClientFactory<DefaultClient> factory =
            new VitamClientFactory<DefaultClient>(configuration, BASE_URI) {

                @Override
                public DefaultClient getClient() {
                    return new DefaultClient(this);
                }

            };
        try (DefaultClient client = factory.getClient()) {
            // Only Apache Pool has this, not the JerseyClient
            assertNull(client.getHttpClient().getHostnameVerifier());
        }
    }

    /**
     * Change client configuration from a Yaml files
     *
     * @param configurationPath the path to the configuration file
     */
    static final SecureClientConfiguration changeConfigurationFile(String configurationPath) {
        SecureClientConfiguration configuration = null;
        try {
            configuration = PropertiesUtils.readYaml(PropertiesUtils.findFile(configurationPath),
                SecureClientConfigurationImpl.class);
        } catch (final IOException e) {
            throw new IllegalStateException("Configuration cannot be read: " + configurationPath, e);
        }
        if (configuration == null) {
            throw new IllegalStateException("Configuration cannot be read: " + configurationPath);
        }
        return configuration;
    }

    @Test
    public void givenCertifValidThenReturnOK() {
        LOGGER.warn("Start Client configuration: " + factory);
        if (application.getVitamServer().isStarted()) {
            try (final DefaultClient client = factory.getClient()) {
                client.checkStatus();
            } catch (final VitamException e) {
                LOGGER.error("THIS SHOULD NOT RAIZED AN EXCEPTION", e);
                fail("THIS SHOULD NOT RAIZED AN EXCEPTION");
            }
        }
    }


    @Test
    public void testIterator() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator<ObjectNode> iterator =
            new VitamRequestIterator<>(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            builder = Response.status(Status.OK);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final JsonNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            // Ensure next hasNext should be False without exception
            when(mock.get()).thenReturn(Response.status(Status.BAD_REQUEST.getStatusCode()).build());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorEmpty() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator iterator =
            new VitamRequestIterator(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK();
            final ResponseBuilder builder = Response.status(Status.NOT_FOUND);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, null).entity(response).build());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorFailed() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator iterator =
            new VitamRequestIterator(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK();
            final ResponseBuilder builder = Response.status(Status.BAD_REQUEST);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, null).entity(response).build());
            try {
                assertFalse(iterator.hasNext());
                fail("should raized an exception");
            } catch (final BadRequestException e) {

            }
        }
    }

    @Test
    public void testIteratorShortList() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator<ObjectNode> iterator =
            new VitamRequestIterator<>(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            final ResponseBuilder builder = Response.status(Status.OK);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, null).entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorThirdShort() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator<ObjectNode> iterator =
            new VitamRequestIterator<>(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            builder = Response.status(Status.NOT_FOUND);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").build());
            assertFalse(iterator.hasNext());
        }
    }

    @Test
    public void testIteratorStopBefore() {
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator iterator =
            new VitamRequestIterator(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            final ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            iterator.close();
            assertFalse(iterator.hasNext());
        }
        startup = true;
        try (final DefaultClient client = factory.getClient();
            VitamRequestIterator<ObjectNode> iterator =
            new VitamRequestIterator<>(client, HttpMethod.GET, "/iterator", ObjectNode.class, null, null)) {
            final RequestResponseOK response = new RequestResponseOK(JsonHandler.createObjectNode());
            final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
            final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
            final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
            response.addResult(node1);
            final List<ObjectNode> list = new ArrayList<>();
            list.add(node2);
            list.add(node3);
            response.addAllResults(list);
            final ResponseBuilder builder = Response.status(Status.PARTIAL_CONTENT);
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            for (int i = 0; i < 3; i++) {
                assertTrue(iterator.hasNext());
                final ObjectNode node = iterator.next();
                assertNotNull(node);
                assertEquals(i + 1, node.get("val").asInt());
            }
            when(mock.get())
                .thenReturn(VitamRequestIterator.setHeaders(builder, true, "newcursor").entity(response).build());
            iterator.close();
            assertFalse(iterator.hasNext());
        }
    }

}
