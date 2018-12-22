package com.senzing.g2.search.elastic;

import java.io.StringReader;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestion;

import com.senzing.g2.engine.G2JNI;
import com.senzing.g2.engine.Result;

public class G2QueryImp implements G2Query
{
	// data objects
	private G2JNI m_g2Engine;
	private ElasticSearchInterface m_searcher;

	// hard-coded values uses for elastic-search
	private int m_maxResultCount = 1000;

    // configurable values used for elastic-search
	private String m_clustername;
	private String m_hostname;
	private int m_portNumber;
	
	// index name identifiers
	private IndexNameKeeper m_indexKeeper;

	@Override
	public int init(
					String moduleName,
					String iniFilename,
					boolean verboseLogging,
					String clustername,
					String hostname,
					int portNumber,
					String indexCollectionName)
	{
		int rVal = 0;

		// save arguments for connecting to the ElasticSearchCluster
		m_clustername = clustername;
		m_hostname = hostname;
		m_portNumber = portNumber;
		
		m_indexKeeper = new IndexNameKeeper(indexCollectionName);

		// Connect to the G2 engine
		m_g2Engine = new G2JNI();
		Result<Long> configID = new Result<Long>();
		rVal = m_g2Engine.init(moduleName, iniFilename, verboseLogging, configID);
		if (rVal != 0)
		{
			return rVal;
		}

		// Connect to the elastic-search cluster
		m_searcher = new ElasticSearchInterface();
		try
		{
			m_searcher.initialize(m_clustername,m_hostname,m_portNumber);
		}
		catch (UnknownHostException e)
		{
			return -1;
		}

		// report success
		return 0;
	}

	@Override
	public int destroy()
	{
		// Disconnect from the G2 JNI
		int rVal = 0;
		if (m_searcher != null)
		{
			m_searcher.shutdown(m_clustername);
			m_searcher = null;
		}
		if (m_g2Engine != null)
		{
			rVal = m_g2Engine.destroy();
			m_g2Engine = null;
		}
		return rVal;
	}

	class ResolvedEntityInfo
	{
		JsonObject entityData;
		double matchScore;
		Map<String,Set<String> > matchFields;

		ResolvedEntityInfo()
		{
			entityData = null;
			matchScore = 0.0;
			matchFields = new TreeMap<String,Set<String> >();
		}
	}

	@Override
	public int queryEntities(String query, StringBuffer response)
	{
		// clear the result buffer
		response.setLength(0);

	    // query for a specific piece of data
		SearchResponse searchResponse = m_searcher.queryDataFromIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities(), query, m_maxResultCount);
		SearchHits searchHits = searchResponse.getHits();

		// declare a collection of search results
		Map<Long,ResolvedEntityInfo> searchResultEntities = new TreeMap<Long,ResolvedEntityInfo>();

		// get the data results for each hit, and dedup for resolved entities
		for (SearchHit hit : searchHits)
		{
			String hitID = hit.getId();
			JsonReader hitIDJsonReader = Json.createReader(new StringReader(hitID));
			JsonObject hitIDEntityDocument = hitIDJsonReader.readObject();
			Map<String, HighlightField> highlights = hit.getHighlightFields();
			float currentHitScore = hit.getScore();

			long resEntID = Long.parseLong(hitIDEntityDocument.getJsonString("ENTITY_ID").getString());
			ResolvedEntityInfo resEntInfo = null;
			if (searchResultEntities.containsKey(resEntID))
			{
				resEntInfo = searchResultEntities.get(resEntID);
			}
			else
			{
				resEntInfo = new ResolvedEntityInfo();
				searchResultEntities.put(resEntID, resEntInfo);
			}
			{
				StringBuffer resolvedEntityData = new StringBuffer();
				int result = m_g2Engine.getEntityByEntityID(resEntID, resolvedEntityData);
				if (result == 0)
				{
					JsonReader resolvedEntityJsonReader = Json.createReader(new StringReader(resolvedEntityData.toString()));
					JsonObject resolvedEntityDocument = resolvedEntityJsonReader.readObject();
					resEntInfo.entityData = resolvedEntityDocument;
					
					resEntInfo.matchScore = Math.max(resEntInfo.matchScore,currentHitScore);
					for (Entry<String, HighlightField> highlight : highlights.entrySet())
					{
						Text[] fragments = highlight.getValue().fragments();
						for (Text fragment : fragments)
						{
							Set<String> matchedFields = null;
							if (resEntInfo.matchFields.containsKey(highlight.getKey()))
							{
								matchedFields = resEntInfo.matchFields.get(highlight.getKey());
							}
							else
							{
								matchedFields = new TreeSet<String>();
								resEntInfo.matchFields.put(highlight.getKey(), matchedFields);
							}
							matchedFields.add(fragment.string());
						}
					}
				}
				else
				{
					searchResultEntities.remove(resEntID);
				}
			}
		}

		// get the resolved entity hits sorted by score
		Vector<ResolvedEntityInfo> resolvedEntityVector
			= new Vector<ResolvedEntityInfo>(searchResultEntities.values());
		Collections.sort(resolvedEntityVector, new Comparator<ResolvedEntityInfo>() {
		    public int compare(ResolvedEntityInfo first, ResolvedEntityInfo second) {
		    	// sort them in descending order by score
		    	return Double.compare(second.matchScore, first.matchScore);
		    }
		});

		// build a response document from the result entities
		JsonArrayBuilder recordResultArray = Json.createArrayBuilder();
		for (ResolvedEntityInfo entityInfo : resolvedEntityVector)
		{
			JsonObjectBuilder recordNodeBuilder
				= Json.createObjectBuilder()
					.add("MATCH_SCORE", entityInfo.matchScore)
					.add("ENTITY_DATA",entityInfo.entityData);
			JsonObjectBuilder highlightNodeBuilder
				= Json.createObjectBuilder();
			for (Entry<String, Set<String>> matchedFieldsForKey : entityInfo.matchFields.entrySet())
			{
				// prepare the highlighted key
				String highlightKey = matchedFieldsForKey.getKey();
				if (highlightKey.startsWith("JSON_DATA."))
				{
					highlightKey = highlightKey.replaceFirst("JSON_DATA\\.", "RECORDS.JSON_DATA.");
				}
				String suffixForKeyword = ".keyword";
				if (highlightKey.endsWith(suffixForKeyword))
				{
					highlightKey = highlightKey.substring(0, highlightKey.length() - suffixForKeyword.length());
				}
				
				// prepare the highlighted result field value
				JsonArrayBuilder fragmentArray = Json.createArrayBuilder();
				for (String matchedField : matchedFieldsForKey.getValue())
				{
					fragmentArray.add(matchedField);
				}
				
				// append it to the results
				highlightNodeBuilder.add(highlightKey, fragmentArray);
			}
			recordNodeBuilder.add("MATCH_FIELDS", highlightNodeBuilder);
			recordResultArray.add(recordNodeBuilder);
		}
        JsonObject documentObject
	    	= Json.createObjectBuilder()
	    		.add("QUERY", query)
	    		.add("RESULT_COUNT", searchResultEntities.size())
	    		.add("RESULT_ENTITIES", recordResultArray)
	    		.build();
	    response.append(documentObject.toString());

		// report success
		return 0;
	}

	@Override
	public int typeAheadEntityLookup(String lookaheadPrefix, StringBuffer response)
	{
		// clear the result buffer
		response.setLength(0);

		// declare a collection of search results
		Set<String> resultTerms = new TreeSet<String>();

	    // query for a specific piece of data
		try 
		{
			final String allDataContext = "suggestionName";
			SearchResponse lookAheadResponse = m_searcher.lookAheadForText(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities(), allDataContext, lookaheadPrefix, m_maxResultCount);
			Suggest suggestions = lookAheadResponse.getSuggest();

			// gather the set of terms
			CompletionSuggestion compSuggestion = suggestions.getSuggestion(allDataContext);
			for (CompletionSuggestion.Entry entry : compSuggestion.getEntries())
			{
				List<CompletionSuggestion.Entry.Option> options = entry.getOptions();
				for (CompletionSuggestion.Entry.Option option : options)
				{
					SearchHit hit = option.getHit();
					String hitDocument = hit.getSourceAsString();
					Set<String> resultTermsForHit = new TreeSet<String>();
					JsonFieldValueFinder.findValues(hitDocument, lookaheadPrefix, resultTermsForHit);
					resultTerms.addAll(resultTermsForHit);
				}
			}			
		}
		catch(Exception e)
		{
			// do nothing.  We failed to get results
		}

		// build a response document
		JsonArrayBuilder recordResultArray = Json.createArrayBuilder();
		for (String term : resultTerms)
		{
			recordResultArray.add(term);
		}
        JsonObject documentObject
        	= Json.createObjectBuilder()
        	.add("LOOKAHEAD_PREFIX", lookaheadPrefix)
        	.add("RESULT_COUNT", resultTerms.size())
        	.add("RESULT_TERMS", recordResultArray)
        	.build();
        response.append(documentObject.toString());

		// report success
		return 0;
	}

	/**
	private int queryAllData(StringBuffer response)
	{
		m_searcher.queryAllDataFromIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities(), m_maxResultCount);
	    response.setLength(0);
	    response.append("{ RESULT : \"SUCCESS\"}");
		return 0;
	}
	**/

}
