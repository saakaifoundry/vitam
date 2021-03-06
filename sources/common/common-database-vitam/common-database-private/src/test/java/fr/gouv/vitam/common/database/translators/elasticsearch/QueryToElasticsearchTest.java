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
package fr.gouv.vitam.common.database.translators.elasticsearch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.List;

import org.bson.Document;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.database.builder.query.PathQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

public class QueryToElasticsearchTest {
    private static final VitamLogger LOGGER =
        VitamLoggerFactory.getInstance(QueryToElasticsearchTest.class);

    private static final String exampleElasticsearch = "{ $roots : [ 'id0' ], $query : [ " +
        "{ $and : [ " + "{$exists : 'mavar1'}, " + "{$missing : 'mavar2'}, " + "{$isNull : 'mavar3'}, " +
        "{ $or : [ {$in : { 'mavar4' : [1, 2, 'maval1'] }}, " + "{ $nin : { 'mavar5' : ['maval2', true] } } ] } ] }," +
        "{ $not : [ " + "{ $size : { 'mavar5' : 5 } }, " + "{ $gt : { 'mavar6' : 7 } }, " +
        "{ $lt : { 'mavar7' : 8 } } ] , $exactdepth : 4}," + "{ $not : [ " + "{ $eq : { 'mavar8' : 5 } }, " +
        "{ $ne : { 'mavar9' : 'ab' } }, " + "{ $wildcard : { 'mavar9' : 'ab' } }, " +
        "{ $range : { 'mavar10' : { $gte : 12, $lte : 20} } } ], $depth : 1}, " +
        "{ $and : [ { $term : { 'mavar14' : 'motMajuscule', 'mavar15' : 'simplemot' } } ] }, " +
        "{ $regex : { 'mavar14' : '^start?aa.*' }, $depth : -1 }, " +
        "{ $range : { 'mavar16' : { $gt : 13, $lt : 29} } }," +
        "{ $gte : { 'mavar17' : 100 } }," +
        "{ $lte : { 'mavar18' : 56 } }," +
        "{ $match : { 'mavar19' : 'words' , '$max_expansions' : 1  } }," +
        "{ $match_phrase : { 'mavar20' : 'words', '$max_expansions' : 1 } }," +
        "{ $match_phrase_prefix : { 'mavar21' : 'phrase', '$max_expansions' : 1 } }," +

        "{ $match : { 'mavar19' : 'words' } }," +
        "{ $match_phrase : { 'mavar20' : 'words'} }," +
        "{ $match_phrase_prefix : { 'mavar21' : 'phrase'} }," +

        "{ $prefix : { 'mavar22' : 'phrase' , '$max_expansions' : 1  }}," +
        "{ $mlt : { $fields : [ 'mavar23', 'mavar24' ], $like : 'like_text' } }," +
        "{ $flt : { $fields : [ 'mavar23', 'mavar24' ], $like : 'like_text' } }," +
        "{ $search : { 'mavar25' : 'searchParameter' } }" +
        "], " +
        "$filter : {$offset : 100, $limit : 1000, $hint : ['cache'], " +
        "$orderby : { maclef1 : 1 , maclef2 : -1,  maclef3 : 1 } }," +
        "$projection : {$fields : {#dua : 1, #all : 1}, $usage : 'abcdef1234' } }";

    private static JsonNode example;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        example = JsonHandler.getFromString(exampleElasticsearch);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {}

    private SelectParserMultiple createSelect() {
        try {
            final SelectParserMultiple request1 = new SelectParserMultiple();
            request1.parse(example);
            assertNotNull(request1);
            return request1;
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
            return null;
        }
    }

    @Test
    public void testGetCommands() {
        try {
            VitamCollection.setMatch(false);
            final SelectParserMultiple parser = createSelect();
            final SelectMultiQuery select = parser.getRequest();
            final QueryBuilder queryBuilderRoot = QueryToElasticsearch.getRoots("_up", select.getRoots());
            final List<SortBuilder> sortBuilders = QueryToElasticsearch.getSorts(parser,
                parser.hasFullTextQuery() || VitamCollection.containMatch(), true);
            VitamCollection.setMatch(false);
            assertEquals(4, sortBuilders.size());

            final List<Query> list = select.getQueries();
            for (int i = 0; i < list.size(); i++) {
                System.out.println(i + " = " + list.get(i).toString());
                final QueryBuilder queryBuilderCommand = QueryToElasticsearch.getCommand(list.get(i));
                final QueryBuilder queryBuilderseudoRequest =
                    QueryToElasticsearch.getFullCommand(queryBuilderCommand, queryBuilderRoot);
                System.out.println(i + " = " + ElasticsearchHelper.queryBuilderToString(queryBuilderseudoRequest));
            }
        } catch (final Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void shouldNotRaiseException_whenPathAllowed()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final Query query = new PathQuery("id0");
        QueryToElasticsearch.getCommand(query);
    }

}
