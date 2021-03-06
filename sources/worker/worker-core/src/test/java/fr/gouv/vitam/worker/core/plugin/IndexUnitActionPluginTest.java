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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.stream.XMLStreamException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.metadata.api.exception.MetaDataAlreadyExistException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClientRest;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, MetaDataClientFactory.class})
public class IndexUnitActionPluginTest {

    IndexUnitActionPlugin plugin = new IndexUnitActionPlugin();
    private WorkspaceClient workspaceClient;
    private MetaDataClient metadataClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private static final String ARCHIVE_UNIT = "indexUnitActionHandler/archiveUnit.json";
    private static final String ARCHIVE_UNIT_WITH_RULES = "indexUnitActionHandler/ARCHIVE_UNIT_TO_INDEX_WITH_RULES.json";
    private static final String ARCHIVE_UNIT_UPDATE_GUID_CHILD = "indexUnitActionHandler/GUID_ARCHIVE_UNIT_CHILD.json";
    private static final String ARCHIVE_UNIT_UPDATE_GUID_PARENT = "indexUnitActionHandler/GUID_ARCHIVE_UNIT_PARENT.json";
    private static final String ARCHIVE_UNIT_WITh_MGT_RULES = "indexUnitActionHandler/ARCHIVE_UNIT_WITH_MGT_RULES.json";
    private static final String ATR_GLOBAL_SEDA_PARAMETERS = "globalSEDAParameters.json";  

    
    private final InputStream archiveUnit;
    private final InputStream archiveUnitWithRules;
    private final InputStream archiveUnitChild;
    private final InputStream archiveUnitParent;
    private final InputStream archiveUnitWithMgtRules;
    private GUID guid = GUIDFactory.newGUID();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    private HandlerIO handlerIO = mock(HandlerIO.class);
    private File globalSEDAParameter;

    private final WorkerParameters params =
        WorkerParametersFactory.newWorkerParameters().setUrlWorkspace("http://localhost:8083")
            .setUrlMetadata("http://localhost:8083")
            .setObjectName("objectName.json").setCurrentStep("currentStep")
            .setContainerName(guid.getId()).setLogbookTypeProcess(LogbookTypeProcess.INGEST);

    public IndexUnitActionPluginTest() throws FileNotFoundException {
        archiveUnit = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT);
        archiveUnitWithRules = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_WITH_RULES);
        archiveUnitChild = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_UPDATE_GUID_CHILD);
        archiveUnitParent = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_UPDATE_GUID_PARENT);
        archiveUnitWithMgtRules = PropertiesUtils.getResourceAsStream(ARCHIVE_UNIT_WITh_MGT_RULES);
    }

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        PowerMockito.mockStatic(MetaDataClientFactory.class);
        workspaceClient = mock(WorkspaceClient.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        metadataClient = mock(MetaDataClientRest.class);
        globalSEDAParameter = (File) PropertiesUtils.getResourceFile(ATR_GLOBAL_SEDA_PARAMETERS);

        when(handlerIO.getInput(0)).thenReturn("INGEST");
        when(handlerIO.getInput(1)).thenReturn(globalSEDAParameter);

    }

    @Test
    public void givenWorkspaceNotExistWhenExecuteThenReturnResponseFATAL()
        throws XMLStreamException, IOException, ProcessingException {

        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        final WorkspaceClientFactory mockedWorkspaceFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(mockedWorkspaceFactory);
        PowerMockito.when(mockedWorkspaceFactory.getClient()).thenReturn(workspaceClient);

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.FATAL);
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenReturn(JsonHandler.createObjectNode());
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        when(handlerIO.getInputStreamNoCachedFromWorkspace(anyObject())).thenReturn(Response.status(Status.OK).entity(archiveUnit).build());
        
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void testMetadataException() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenThrow(new MetaDataExecutionException(""));
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);        
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);        
        when(handlerIO.getInputStreamNoCachedFromWorkspace(anyObject())).thenReturn(Response.status(Status.OK).entity(archiveUnit).build());        
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.FATAL);
    }

    @Test
    public void testWorkspaceException() throws Exception {
        when(metadataClient.insertUnit(anyObject())).thenReturn(JsonHandler.createObjectNode());
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        when(workspaceClient.getObject(anyObject(), eq("Units/objectName.json")))
            .thenThrow(new ContentAddressableStorageNotFoundException(""));
        
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.FATAL);
    }


    @Test
    public void testIndexUnitWithRulesOk()
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataAlreadyExistException,
        MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException,
        ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        when(metadataClient.insertUnit(anyObject())).thenReturn(JsonHandler.createObjectNode());
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        when(handlerIO.getInputStreamNoCachedFromWorkspace(anyObject())).thenReturn(Response.status(Status.OK).entity(archiveUnitWithRules).build());
        
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void testIndexUnitUpdateChildOk()
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataAlreadyExistException,
        MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException,
        ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        when(metadataClient.insertUnit(anyObject())).thenReturn(JsonHandler.createObjectNode());
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);        
        when(handlerIO.getInputStreamNoCachedFromWorkspace(anyObject())).thenReturn(Response.status(Status.OK).entity(archiveUnitChild).build());
        
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

    @Test
    public void testIndexUnitUpdateParentOk()
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataAlreadyExistException,
        MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException,
        ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        when(metadataClient.insertUnit(anyObject())).thenReturn(JsonHandler.createObjectNode());
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        when(handlerIO.getInputStreamNoCachedFromWorkspace(anyObject())).thenReturn(Response.status(Status.OK).entity(archiveUnitParent).build());        

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }
    
    @Test
    public void testIndexWithRules()
        throws MetaDataExecutionException, MetaDataNotFoundException, MetaDataAlreadyExistException,
        MetaDataDocumentSizeException, MetaDataClientServerException, InvalidParseOperationException,
        ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException {

        when(metadataClient.insertUnit(anyObject())).thenReturn(JsonHandler.createObjectNode());
        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        when(handlerIO.getInputStreamNoCachedFromWorkspace(anyObject())).thenReturn(Response.status(Status.OK).entity(archiveUnitWithMgtRules).build());

        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(response.getGlobalStatus(), StatusCode.OK);
    }

}
