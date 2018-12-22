package com.senzing.g2.search.elastic;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.ListIterator;

import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;
import org.elasticsearch.transport.client.PreBuiltTransportClient;


public class ElasticSearchInterface
{
	private TransportClient mClient = null;
	private TransportClient mPreBuiltTransportClient = null;

    public void initialize(String clustername, String hostname, int portNumber) throws UnknownHostException
    {
    	Settings settings = Settings.builder()
    	        .put("cluster.name", clustername).build();
    	mPreBuiltTransportClient = new PreBuiltTransportClient(settings);
		mClient
    		= mPreBuiltTransportClient
    	        .addTransportAddress(new TransportAddress(InetAddress.getByName(hostname), portNumber));
	}

    public void shutdown(String clustername)
    {
    	mClient.close();
    	mPreBuiltTransportClient.close();
	}

	public void createIndex(String indexName, String indexType)
	{
		String mappingTemplate 
			= "{"
			+ "  \""+indexType+"\": "
			+ "  {"
			+ "    \"dynamic_templates\":"
			+ "    ["
			+ "      {"
			+ "        \"g2_named_field_template_string\":"
			+ "        {"
			+ "          \"match\": \"*\","
			+ "          \"match_mapping_type\": \"string\","
			+ "          \"mapping\":"
			+ "          {"
			+ "            \"type\": \"text\","
			+ "            \"copy_to\": \"ALL_G2_DATA\","
			+ "            \"fields\":"
			+ "            {"
			+ "              \"keyword\":"
			+ "              {"
			+ "                \"type\": \"keyword\","
			+ "                \"ignore_above\": 256"
			+ "              },"
			+ "              \"typeahead_suggest\":"
			+ "              {"
			+ "                \"analyzer\": \"standard\","  // the analyzer is "simple" by default, but that removes numbers, so we must use "standard"
			+ "                \"type\": \"completion\""
			+ "              }"
			+ "            }"
			+ "          }"
			+ "        }"
			+ "      }"
			+ "    ]"
			+ "  }"
			+ "}"
			;
		
		//System.out.println(mappingTemplate);
		// create the new index, with the mapping included
		CreateIndexResponse thing 
			= mClient.admin().indices().prepareCreate(indexName)
				.addMapping(indexType,mappingTemplate, XContentType.JSON)
				.execute().actionGet(); 
	}

	public boolean indexExists(String indexName)
	{
		return mClient.admin().indices().prepareExists(indexName).get().isExists();
	}

	public void deleteIndex(String indexName, String indexType)
	{
		mClient.admin().indices().prepareDelete(indexName).get();
	}

	public void flushDataToIndex(String indexName)
	{
		mClient.admin().indices().prepareRefresh(indexName).get();
	}

	public void addDataToIndex(String indexName, String type, String id, String jsonData)
	{
	    mClient.prepareIndex(indexName, type, id).setSource(JsonStringifier.stringifyJson(jsonData), XContentType.JSON).get();
	}
	
	public void updateDataInIndex(String indexName, String type, String id, String jsonData)
	{
		mClient.prepareUpdate(indexName, type, id).setDoc(JsonStringifier.stringifyJson(jsonData), XContentType.JSON).get();
	}

	public void addBulkDataToIndex(String indexName, String indexType, List<ElasticSearchDocumentInfo> docs)
	{
		BulkRequestBuilder bulkRequest = mClient.prepareBulk();
		ListIterator<ElasticSearchDocumentInfo> docInfoIter = docs.listIterator();
        while(docInfoIter.hasNext())
        {
        	ElasticSearchDocumentInfo docInfo = docInfoIter.next();
    		bulkRequest.add(mClient.prepareIndex(indexName, indexType, docInfo.getID()).setSource(JsonStringifier.stringifyJson(docInfo.getJsonData()), XContentType.JSON));
        }
		BulkResponse bulkResponse = bulkRequest.get();
		if (bulkResponse.hasFailures()) 
		{
			// we should probably log these better
			System.err.println("Failures occurred during bulk data load ["+bulkResponse.buildFailureMessage()+"]");
		}
	}

	public void deleteBulkDataFromIndex(String indexName, String indexType, List<ElasticSearchDocumentInfo> IDs)
	{
		BulkRequestBuilder bulkRequest = mClient.prepareBulk();
		ListIterator<ElasticSearchDocumentInfo> idIter = IDs.listIterator();
        while(idIter.hasNext())
        {
        	ElasticSearchDocumentInfo idInfo = idIter.next();
        	bulkRequest.add(mClient.prepareDelete(indexName, indexType, idInfo.getID()));
        }
		bulkRequest.get();
	}

	public void deleteDataFromIndex(String indexName, String type, String id)
	{
		mClient.prepareDelete(indexName, type, id).get();
	}

	public void getDataFromIndex(String indexName, String type, String id)
	{
		mClient.prepareGet(indexName,type,id).get();
	}

	public void clearAllDataFromIndex(String indexName, String indexType)
	{
		deleteIndex(indexName,indexType);
		createIndex(indexName,indexType);
	}

	public SearchResponse queryAllDataFromIndex(String indexName, String type, int maxResultCount)
	{
		SearchResponse response = mClient.prepareSearch(indexName)
				  .setTypes(type)
				  .setSearchType(SearchType.QUERY_THEN_FETCH)
				  .setQuery(QueryBuilders.matchAllQuery())
				  .setSize(maxResultCount)
				  .execute()
				  .actionGet();
		return response;
	}

	public SearchResponse queryDataFromIndex(String indexName, String type, String query, int maxResultCount)
	{
		// This builds a query that just checks for all terms
		//QueryBuilder qb = QueryBuilders.matchQuery("_all",query).fuzziness(Fuzziness.AUTO);

		// This builds a query that checks for each term separately, combining results (using "OR")
		//BoolQueryBuilder qb = QueryBuilders.boolQuery();
		//String[] termList = query.split("\\s");
		//for (String term : termList)
		//{
		//	QueryBuilder termQuery = QueryBuilders.matchQuery("_all", term).fuzziness(Fuzziness.AUTO);
		//	qb.should(termQuery);
		//}

		// This builds a query that checks for each term separately, combining results (using "AND")
		BoolQueryBuilder qb = QueryBuilders.boolQuery();
		String[] termList = query.split("\\s");
		for (String term : termList)
		{
			QueryBuilder termQuery = QueryBuilders.matchQuery("ALL_G2_DATA", term).fuzziness(Fuzziness.AUTO);
			qb.must(termQuery);
		}

		HighlightBuilder hb
			= new HighlightBuilder()
				.field("*")
				.preTags("<G2_MATCHED_FIELD>")
				.postTags("</G2_MATCHED_FIELD>")
				.requireFieldMatch(false);
		SearchRequestBuilder requestBuilder = 
		  mClient.prepareSearch(indexName)
				  .setTypes(type)
				  .setSearchType(SearchType.QUERY_THEN_FETCH)
				  .setQuery(qb)
				  .highlighter(hb)
				  .setSize(maxResultCount);
		SearchResponse response 
		  = requestBuilder
				  .execute()
				  .actionGet();

		return response;
	}

	public SearchResponse lookAheadForText(String indexName, String indexType, String dataContext, String lookaheadPrefix, int maxResultCount)
	{
		CompletionSuggestionBuilder suggestionBuilder
			= SuggestBuilders.completionSuggestion("ALL_G2_DATA.typeahead_suggest")
				.prefix(lookaheadPrefix).size(maxResultCount);
		SearchResponse response
			= mClient
				.prepareSearch(indexName)
				.setSearchType(SearchType.QUERY_THEN_FETCH)
				.suggest(new SuggestBuilder().addSuggestion(dataContext, suggestionBuilder))
				.execute()
				.actionGet();
		return response;
	}
}
