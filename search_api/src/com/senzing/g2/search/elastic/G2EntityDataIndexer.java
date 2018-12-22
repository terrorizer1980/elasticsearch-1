package com.senzing.g2.search.elastic;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.ResourceAlreadyExistsException;


public class G2EntityDataIndexer 
{
	// data objects
	private ElasticSearchInterface m_elasticSearchInstance;

    // connection values used for elastic-search
	private String m_clustername;
	private String m_hostname;
	private int m_portNumber;
	
	// index name identifiers
	private IndexNameKeeper m_indexKeeper;

	public int init(
			String moduleName,
			String iniFilename,
			boolean verboseLogging,
			String clustername,
			String hostname,
			int portNumber,
			String indexCollectionName)
	{
		// save arguments for connecting to the ElasticSearchCluster
		m_clustername = clustername;
		m_hostname = hostname;
		m_portNumber = portNumber;
		
		m_indexKeeper = new IndexNameKeeper(indexCollectionName);
		
		// Connect to the elastic-search cluster
		m_elasticSearchInstance = new ElasticSearchInterface();
		try
		{
			m_elasticSearchInstance.initialize(m_clustername,m_hostname,m_portNumber);
		}
		catch (UnknownHostException e)
		{
			return -1;
		}
		
		// report success
		return 0;
	}
	
	public int destroy()
	{
		// Disconnect from the G2 search interface
		int rVal = 0;
		if (m_elasticSearchInstance != null)
		{
			m_elasticSearchInstance.shutdown(m_clustername);
			m_elasticSearchInstance = null;
		}
		return rVal;
	}

	public boolean createIndex()
	{
		boolean success = false;
		try
		{
			m_elasticSearchInstance.createIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities());
			success = true;
		}
		catch (ResourceAlreadyExistsException e)
		{
			// Ignore this.  We overwrite the existing index
		}
		return success;
	}

	public boolean deleteIndex()
	{
		boolean success = false;
		try
		{
			m_elasticSearchInstance.deleteIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities());
			success = true;
		}
		catch (ResourceAlreadyExistsException e)
		{
			// Ignore this.  We overwrite the existing index
		}
		return success;
	}

	public boolean indexExists()
	{
		return m_elasticSearchInstance.indexExists(m_indexKeeper.getIndexNameForEntities());
	}

	public void clearAllEntityDataFromIndex()
	{
		m_elasticSearchInstance.clearAllDataFromIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities());
	}

	public void flushEntityDataToIndex()
	{
		m_elasticSearchInstance.flushDataToIndex(m_indexKeeper.getIndexNameForEntities());
	}

	public void addBulkEntityDataToIndex(List<G2EntityInfo> docs)
	{
		List<ElasticSearchDocumentInfo> indexedDocs = new ArrayList<ElasticSearchDocumentInfo>(docs.size());
		for (G2EntityInfo resolvedEntityInfo : docs)
		{
			G2EntitySearchData searchData = resolvedEntityInfo.getEntitySearchData();
			indexedDocs.add(new ElasticSearchDocumentInfo(searchData.getElasticSearchEntityIdentifier(),searchData.getRecordData()));
		}
		m_elasticSearchInstance.addBulkDataToIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities(),indexedDocs);
	}

	public void addEntityDataToIndex(G2EntityInfo resolvedEntityInfo)
	{
		G2EntitySearchData searchData = resolvedEntityInfo.getEntitySearchData();
		m_elasticSearchInstance.addDataToIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities(), searchData.getElasticSearchEntityIdentifier(),searchData.getRecordData());
	}

	public void updateEntityDataInIndex(G2EntityInfo resolvedEntityInfo)
	{
		G2EntitySearchData searchData = resolvedEntityInfo.getEntitySearchData();
		m_elasticSearchInstance.updateDataInIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities(), searchData.getElasticSearchEntityIdentifier(),searchData.getRecordData());
	}

	public void deleteEntityDataFromIndex(G2EntityInfo resolvedEntityInfo)
	{
		G2EntitySearchData searchData = resolvedEntityInfo.getEntitySearchData();
		m_elasticSearchInstance.deleteDataFromIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities(),searchData.getElasticSearchEntityIdentifier());
	}

	public void deleteBulkEntityDataFromIndex(List<G2EntityInfo> IDs)
	{
		List<ElasticSearchDocumentInfo> indexedDocs = new ArrayList<ElasticSearchDocumentInfo>(IDs.size());
		for (G2EntityInfo resolvedEntityInfo : IDs)
		{
			G2EntitySearchData searchData = resolvedEntityInfo.getEntitySearchData();
			indexedDocs.add(new ElasticSearchDocumentInfo(searchData.getElasticSearchEntityIdentifier(),searchData.getRecordData()));
		}
		m_elasticSearchInstance.deleteBulkDataFromIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities(),indexedDocs);
	}

	public void getEntityDataFromIndex(G2EntityInfo resolvedEntityInfo)
	{
		G2EntitySearchData searchData = resolvedEntityInfo.getEntitySearchData();
		m_elasticSearchInstance.getDataFromIndex(m_indexKeeper.getIndexNameForEntities(),m_indexKeeper.getIndexTypeForEntities(),searchData.getElasticSearchEntityIdentifier());
	}

}


