package com.senzing.g2.search.elastic.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.ResourceAlreadyExistsException;

import com.senzing.g2.engine.G2JNI;
import com.senzing.g2.engine.Result;
import com.senzing.g2.search.elastic.G2EntityDataIndexer;
import com.senzing.g2.search.elastic.G2EntityInfo;
import com.senzing.g2.search.elastic.G2Query;
import com.senzing.g2.search.elastic.G2QueryImp;

public class G2IndexingFromExportDataApp 
{
	public static void main(String[] args)
	{
		System.out.println("Program started.");
		try {
			runApp(args);
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		System.out.println("Program complete.");
	}

	public static void runApp(String[] args) throws IOException
	{
		// define G2 connecting information
		String moduleName = "G2ElasticSearch";
		String iniFilename = "g2.ini";
		boolean verboseLogging = false;

		// define information for loading data
		String jsonDataFileName = "resources/sample_people.json";
		String dataSource = "PERSON";
		String loadID = "PERSON_LOAD";

		// define ElasticSearch index information
	    String elasticSearchClustername = "elasticsearch";
	    String elasticSearchHostname = "localhost";
	    int elasticSearchPortNumber = 9300;
	    String elasticSearchIndexName = "g2searchindex";

	    /////////////////////////////////////////////
	    // build the index from our G2 data
	    /////////////////////////////////////////////
	    {
			int returnValue = 0;

			// Connect to the G2 Engine
			System.out.println("Connecting to G2 engine.");
			G2JNI g2Engine = new G2JNI();
			Result<Long> configID = new Result<Long>();
			returnValue = g2Engine.init(moduleName, iniFilename, verboseLogging,configID);
			if (returnValue != 0)
			{
				System.out.println("Could not connect to G2");
				System.out.println("Return Code = "+returnValue);
				System.out.println("Exception Code = "+g2Engine.getLastExceptionCode());
				System.out.println("Exception = "+g2Engine.getLastException());
				return;
			}

			// purge the repository (if so desired)
			System.out.println("Purging G2 repository.");
			g2Engine.purgeRepository();
			
			// load the data into G2
			System.out.println("Loading data into G2 repository.");
			try
			{
				List<G2EntityInfo> entitiesToIndex = new LinkedList<G2EntityInfo>();
				final int G2_RECORD_LOAD_INTERVAL = 500;
				int recordsLoaded = 0;
				File jsonDataInputFile = new File(jsonDataFileName); 
				BufferedReader inputBufferedReader = new BufferedReader(new FileReader(jsonDataInputFile)); 
				String jsonRecord; 
				while ((jsonRecord = inputBufferedReader.readLine()) != null)
				{
					if (!jsonRecord.isEmpty())
					{ 
					    StringBuffer recordID = new StringBuffer();
					    g2Engine.addRecordWithReturnedRecordID(dataSource, recordID, jsonRecord, loadID);
						++recordsLoaded;
					}
					if (recordsLoaded % G2_RECORD_LOAD_INTERVAL == 0)
					{
						System.out.println(recordsLoaded+" records loaded...");
					}
				}
				System.out.println("Finished loading records.");
			}
			catch (Exception e)
			{
				System.out.println("Failed to index the data");
				System.out.println("Exception = "+e);
				return;
			}
			
			// initialize the indexer object
			System.out.println("Initializing search indexer interface.");
		    G2EntityDataIndexer entityIndexer = new G2EntityDataIndexer();
		    returnValue = entityIndexer.init(moduleName,iniFilename,verboseLogging,elasticSearchClustername,elasticSearchHostname,elasticSearchPortNumber,elasticSearchIndexName);
			if (returnValue != 0)
			{
				System.out.println("Could not initialize the indexer object");
				System.out.println("Return Code = "+returnValue);
				return;
			}

			// Create an elastic-search index
			System.out.println("Creating search index.");
			try
			{
				entityIndexer.createIndex();
			}
			catch (ResourceAlreadyExistsException e)
			{
				// clear the existing entity data from the index, if desired.
				entityIndexer.clearAllEntityDataFromIndex();
			}
			
			// purge the search index (if so desired)
			System.out.println("Purging search index.");
			entityIndexer.clearAllEntityDataFromIndex();
			
			// load the data into the search index
			System.out.println("Loading data to search index.");
			try
			{
				List<G2EntityInfo> docsToIndex = new LinkedList<G2EntityInfo>();
				final int BULK_INSERT_SIZE = 500;
				long exportHandle = g2Engine.exportJSONEntityReport(5);
				int entitiesIndexed = 0;
				String jsonDataString = g2Engine.fetchNext(exportHandle);
				while (jsonDataString != null)
				{
					docsToIndex.add(new G2EntityInfo(jsonDataString));
					++entitiesIndexed;						
					if (entitiesIndexed % BULK_INSERT_SIZE == 0)
					{
						entityIndexer.addBulkEntityDataToIndex(docsToIndex);
						docsToIndex.clear();
						System.out.println(entitiesIndexed+" entities indexed...");
					}
					jsonDataString = g2Engine.fetchNext(exportHandle);
				}
				entityIndexer.addBulkEntityDataToIndex(docsToIndex);
				docsToIndex.clear();
				System.out.println("Finished indexing entities.");
			}
			catch (Exception e)
			{
				System.out.println("Failed to index the data");
				System.out.println("Exception = "+e);
				return;
			}
			entityIndexer.flushEntityDataToIndex();
		
			// close the search indexer
			System.out.println("Closing search indexer interface.");
			returnValue = entityIndexer.destroy();
			if (returnValue != 0)
			{
				System.out.println("Could not disconnect from G2");
				System.out.println("Return Code = "+returnValue);
				return;
			}
			entityIndexer = null;

			// close the G2 engine instance
			System.out.println("Closing G2 engine interface.");
			if (g2Engine != null)
			{
				returnValue = g2Engine.destroy();
				if (returnValue != 0)
				{
					System.out.println("Could not disconnect from G2");
					System.out.println("Return Code = "+returnValue);
					System.out.println("Exception Code = "+g2Engine.getLastExceptionCode());
					System.out.println("Exception = "+g2Engine.getLastException());
					return;
				}
				g2Engine = null;
			}
	    }


	    /////////////////////////////////////////////
	    // Search the index for desired values
	    /////////////////////////////////////////////
	    {
			int returnValue = 0;

			// initialize the searcher interface
			System.out.println("Initializing G2 search interface.");
		    G2Query searcher1 = new G2QueryImp();
		    returnValue = searcher1.init(moduleName,iniFilename,verboseLogging,elasticSearchClustername,elasticSearchHostname,elasticSearchPortNumber,elasticSearchIndexName);
			if (returnValue != 0)
			{
				System.out.println("Could not initialize the G2 query module");
				System.out.println("Return Code = "+returnValue);
				return;
			}
			
			//////////////////////////////
			// Search for entity data
			//////////////////////////////		

			System.out.println("Performing data searching...");
			System.out.println();

			// search for a single term
			{
				StringBuffer searchResponse = new StringBuffer();
				String query = "Robert";
				System.out.println("Search request string = '"+query+"'");
				returnValue = searcher1.queryEntities(query,searchResponse);
				if (returnValue != 0)
				{
					System.out.println("Could not search using the G2 module");
					System.out.println("Return Code = "+returnValue);
					return;
				}
				System.out.println("Search response = '"+searchResponse+"'");
				System.out.println();
			}

			// search for a more complex term
			{
				StringBuffer searchResponse = new StringBuffer();
				String query = "ROBERT las vegas 8922";
				System.out.println("Search request = '"+query+"'");
				returnValue = searcher1.queryEntities(query,searchResponse);
				if (returnValue != 0)
				{
					System.out.println("Could not search using the G2 module");
					System.out.println("Return Code = "+returnValue);
					return;
				}
				System.out.println("Search response = '"+searchResponse+"'");
				System.out.println();
			}

			// search for something not in the data
			{
				StringBuffer searchResponse = new StringBuffer();
				String query = "my_pretend_data";
				System.out.println("Search request = '"+query+"'");
				returnValue = searcher1.queryEntities(query,searchResponse);
				if (returnValue != 0)
				{
					System.out.println("Could not search using the G2 module");
					System.out.println("Return Code = "+returnValue);
					return;
				}
				System.out.println("Search response = '"+searchResponse+"'");
				System.out.println();
			}

			System.out.println("Finished data searching...");

			////////////////////////////////////////////////////////////////////////
			// Search for auto-complete terms (also called look-ahead terms)
			////////////////////////////////////////////////////////////////////////

			System.out.println("Performing term look-ahead searching...");
			System.out.println();

			// basic search
			{
				StringBuffer lookaheadResponse = new StringBuffer();
				String lookAheadQuery = "ROB";
				System.out.println("Lookahead query = '"+lookAheadQuery+"'");
				returnValue = searcher1.typeAheadEntityLookup(lookAheadQuery, lookaheadResponse);
				if (returnValue != 0)
				{
					System.out.println("Could not look-ahead using the G2 module");
					System.out.println("Return Code = "+returnValue);
					return;
				}
				System.out.println("Lookahead response = '"+lookaheadResponse+"'");
				System.out.println();
			}

			// search with multiple results
			{
				StringBuffer lookaheadResponse = new StringBuffer();
				String lookAheadQuery = "R";
				System.out.println("Lookahead query = '"+lookAheadQuery+"'");
				returnValue = searcher1.typeAheadEntityLookup(lookAheadQuery, lookaheadResponse);
				if (returnValue != 0)
				{
					System.out.println("Could not look-ahead using the G2 module");
					System.out.println("Return Code = "+returnValue);
					return;
				}
				System.out.println("Lookahead response = '"+lookaheadResponse+"'");
				System.out.println();
			}

			// search with no results
			{
				StringBuffer lookaheadResponse = new StringBuffer();
				String lookAheadQuery = "my_pretend_data";
				System.out.println("Lookahead query = '"+lookAheadQuery+"'");
				returnValue = searcher1.typeAheadEntityLookup(lookAheadQuery, lookaheadResponse);
				if (returnValue != 0)
				{
					System.out.println("Could not look-ahead using the G2 module");
					System.out.println("Return Code = "+returnValue);
					return;
				}
				System.out.println("Lookahead response = '"+lookaheadResponse+"'");
				System.out.println();
			}

			System.out.println("Finished term look-ahead searching.");

			// close the searcher interface
			System.out.println("Closing G2 search interface.");
			returnValue = searcher1.destroy();
			if (returnValue != 0)
			{
				System.out.println("Could not destroy the searcher module");
				System.out.println("Return Code = "+returnValue);
				return;
			}
			searcher1 = null;
	    }
	}
}
