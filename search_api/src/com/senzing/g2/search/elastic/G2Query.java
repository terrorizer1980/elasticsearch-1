package com.senzing.g2.search.elastic;

public interface G2Query {
	  /**
	   * Initializes the G2 engine
	   * This should only be called once per process.  Currently re-initializing the G2 engin
	   * after a destroy requires unloaded the class loader used to load this class.
	   *
	   * @param moduleName A short name given to this instance of the engine
	   * @param iniFilename A fully qualified path to the G2 engine INI file (often /opt/senzing/g2/python/G2Module.ini)
	   * @param verboseLogging Enable diagnostic logging which will print a massive amount of information to stdout
	   *
	   * @return 0 on success
	   */
	  int init(String moduleName, 
			  	String iniFilename, 
			  	boolean verboseLogging, 
				String clustername,
				String hostname,
				int portNumber,
				String indexName);


	  /**
	   * Uninitializes the G2 engine.
	   *
	   * @return 0 on success
	   */
	  int destroy();


	  /**
	   * @param query List of tokens, potentially with instance specific formatting (e.g. SOLR), to search for
	   * @param response A memory buffer for returning the JSON response document.  Matching Entities order by
	   *                 degree of match to query.  Fields should be highlighted based on matches.
	   *        If an error occurred, an error response is stored here.
	   *
	   * @return Returns 0 for success
	   */
	  int queryEntities(String query, StringBuffer response);

	  /**
	   * @param typing Current token to lookup for type ahead
	   * @param response A memory buffer for returning the JSON response document. Lists token suggestions
	                     with highest priority first.
	   *        If an error occurred, an error response is stored here.
	   *
	   * @return Returns 0 for success
	   */
	  int typeAheadEntityLookup(String typing, StringBuffer response);


	}