package fr.gouv.vitam.access.external.rest;

import static com.jayway.restassured.RestAssured.given;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.mockito.Matchers.anyObject;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.error.VitamError;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;

import fr.gouv.vitam.access.external.api.AccessExtAPI;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.junit.JunitHelper;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*", "javax.management.*"})
@PrepareForTest({AdminManagementClientFactory.class})
public class AdminManagementExternalResourceImplTest {

    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(AdminManagementExternalResourceImplTest.class);

    private static final String RESOURCE_URI = "/admin-external/v1";

    private static final String FORMAT_URI = "/" + AdminCollections.FORMATS.getName();

    private static final String RULES_URI = "/" + AdminCollections.RULES.getName();

    private static final String DOCUMENT_ID = "/1";

    private static final String WRONG_URI = "/wrong-uri";

    private static final String TENANT_ID = "0";

    private static final String UNEXISTING_TENANT_ID = "25";
    private static final String PROFILE_URI = "/profiles";
    

    private InputStream stream;
    private static JunitHelper junitHelper;
    private static int serverPort;
    private static AccessExternalApplication application;
    private static AdminManagementClient adminCLient;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

        junitHelper = JunitHelper.getInstance();
        serverPort = junitHelper.findAvailablePort();

        RestAssured.port = serverPort;
        RestAssured.basePath = RESOURCE_URI;

        try {
            application = new AccessExternalApplication("access-external-test.conf");
            application.start();
        } catch (final VitamApplicationServerException e) {
            LOGGER.error(e);
            throw new IllegalStateException(
                "Cannot start the Access External Application Server", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (application != null && application.getVitamServer() != null &&
            application.getVitamServer().getServer() != null) {

            application.stop();
        }
        junitHelper.releasePort(serverPort);
    }

    @Test
    public void testCheckDocument() throws FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(RULES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().put(RULES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().put(RULES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(WRONG_URI)
            .then().statusCode(Status.NOT_FOUND.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().put(WRONG_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().put(WRONG_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void testCheckDocumentError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doThrow(new ReferentialException("")).when(adminCLient).checkFormat(anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().put(FORMAT_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());
    }

    @Test
    public void insertDocument() throws FileNotFoundException {
        AdminManagementClientFactory.changeMode(null);
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.CREATED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(RULES_URI)
            .then().statusCode(Status.CREATED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(RULES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .when().post(RULES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(WRONG_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given().contentType(ContentType.BINARY)
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(WRONG_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given().contentType(ContentType.BINARY)
            .when().post(WRONG_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }

    @Test
    public void insertDocumentError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doThrow(new ReferentialException("")).when(adminCLient).importFormat(anyObject());
        doReturn(Status.OK).when(adminCLient).checkFormat(anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        doThrow(new DatabaseConflictException("")).when(adminCLient).importFormat(anyObject());

        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.CONFLICT.getStatusCode());

    }

    @Test
    public void testGetDocuments() throws InvalidCreateOperationException, FileNotFoundException {
        final Select select = new Select();
        select.setQuery(eq("Id", "APP-00001"));
        AdminManagementClientFactory.changeMode(null);

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(RULES_URI + DOCUMENT_ID)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(RULES_URI + DOCUMENT_ID)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().post(RULES_URI + DOCUMENT_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(RULES_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(RULES_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().post(RULES_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI + DOCUMENT_ID)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(FORMAT_URI + DOCUMENT_ID)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().get(FORMAT_URI + DOCUMENT_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI)
            .then().statusCode(Status.OK.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().post(FORMAT_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().post(FORMAT_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(WRONG_URI + DOCUMENT_ID)
            .then().statusCode(Status.NOT_FOUND.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(WRONG_URI + DOCUMENT_ID)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .accept(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().get(WRONG_URI + DOCUMENT_ID)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(WRONG_URI)
            .then().statusCode(Status.NOT_FOUND.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, UNEXISTING_TENANT_ID)
            .when().get(WRONG_URI)
            .then().statusCode(Status.UNAUTHORIZED.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .when().get(WRONG_URI)
            .then().statusCode(Status.PRECONDITION_FAILED.getStatusCode());

    }

    @Test
    public void testGetDocumentsError() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doThrow(new ReferentialException("")).when(adminCLient).getFormats(anyObject());
        doThrow(new ReferentialException("")).when(adminCLient).getFormatByID(anyObject());
        final Select select = new Select();
        select.setQuery(eq("Id", "APP-00001"));

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI + DOCUMENT_ID)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI)
            .then().statusCode(Status.INTERNAL_SERVER_ERROR.getStatusCode());


        doThrow(new InvalidParseOperationException("")).when(adminCLient).getFormats(anyObject());
        doThrow(new InvalidParseOperationException("")).when(adminCLient).getFormatByID(anyObject());

        given()
            .accept(ContentType.JSON)
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI + DOCUMENT_ID)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

        given()
            .contentType(ContentType.JSON)
            .body(select.getFinalSelect())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(FORMAT_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode());

    }

    @Test
    public void testImportIngestContractsWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(Status.BAD_REQUEST).when(adminCLient).importIngestContracts(anyObject());
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ENTRY_CONTRACT_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");

    }


    @Test
    public void testimportValidIngestContractsFileReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(Status.CREATED).when(adminCLient).importIngestContracts(anyObject());
        stream = PropertiesUtils.getResourceAsStream("referential_contracts_ok.json");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ENTRY_CONTRACT_API)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testfindIngestContractsFile() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);

        doReturn(new RequestResponseOK<>().addAllResults(getIngestContracts())).when(adminCLient).findIngestContracts(anyObject());
        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
                .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
                .when().get(AccessExtAPI.ENTRY_CONTRACT_API)
        .then().statusCode(Status.OK.getStatusCode());
    }


    @Test
    public void testImportAccessContractsWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(Status.BAD_REQUEST).when(adminCLient).importAccessContracts(anyObject());
        stream = PropertiesUtils.getResourceAsStream("vitam.conf");
        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");

    }


    @Test
    public void testimportValidAccessContractsFileReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(Status.CREATED).when(adminCLient).importAccessContracts(anyObject());
        stream = PropertiesUtils.getResourceAsStream("contracts_access_ok.json");

        given().contentType(ContentType.BINARY).body(stream)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.CREATED.getStatusCode());
    }

    @Test
    public void testfindAccessContractsFile() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(new RequestResponseOK<>().addAllResults(getAccessContracts())).when(adminCLient).findAccessContracts(anyObject());
        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(AccessExtAPI.ACCESS_CONTRACT_API)
            .then().statusCode(Status.OK.getStatusCode());
    }



    @Test
    public void testCreateProfileWithInvalidFileBadRequest() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(new VitamError("").setHttpCode(Status.BAD_REQUEST.getStatusCode())).when(adminCLient).createProfiles(anyObject());

        File fileProfiles = PropertiesUtils.getResourceFile("profile_missing_identifier.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);

        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(PROFILE_URI)
            .then().statusCode(Status.BAD_REQUEST.getStatusCode()).contentType("application/json");

    }


    @Test
    public void testcreateValidProfileReturnCreated() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(new RequestResponseOK<>().setHttpCode(Status.CREATED.getStatusCode())).when(adminCLient).createProfiles(anyObject());

        File fileProfiles = PropertiesUtils.getResourceFile("profiles_ok.json");
        JsonNode json = JsonHandler.getFromFile(fileProfiles);



        given().contentType(ContentType.JSON).body(json)
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().post(PROFILE_URI)
            .then().statusCode(Status.CREATED.getStatusCode()).contentType("application/json");;
    }

    @Test
    public void testfindProfiles() throws Exception {
        PowerMockito.mockStatic(AdminManagementClientFactory.class);
        adminCLient = PowerMockito.mock(AdminManagementClient.class);
        final AdminManagementClientFactory adminClientFactory = PowerMockito.mock(AdminManagementClientFactory.class);
        when(AdminManagementClientFactory.getInstance()).thenReturn(adminClientFactory);
        when(AdminManagementClientFactory.getInstance().getClient()).thenReturn(adminCLient);
        doReturn(new RequestResponseOK<>().addAllResults(getAccessContracts())).when(adminCLient).findProfiles(anyObject());
        given().contentType(ContentType.JSON).body(JsonHandler.createObjectNode())
            .header(GlobalDataRest.X_TENANT_ID, TENANT_ID)
            .when().get(PROFILE_URI)
            .then().statusCode(Status.OK.getStatusCode());
    }



    private List<Object> getIngestContracts() throws FileNotFoundException, InvalidParseOperationException {
    	InputStream fileContracts = PropertiesUtils.getResourceAsStream("referential_contracts_ok.json");
		ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContracts);
		List<Object> res = new ArrayList<>();
		array.forEach(e -> res.add(e));
		return res;
	}

    private List<Object> getAccessContracts() throws FileNotFoundException, InvalidParseOperationException {
        InputStream fileContracts = PropertiesUtils.getResourceAsStream("contracts_access_ok.json");
        ArrayNode array = (ArrayNode) JsonHandler.getFromInputStream(fileContracts);
        List<Object> res = new ArrayList<>();
        array.forEach(e -> res.add(e));
        return res;
    }
    

}
