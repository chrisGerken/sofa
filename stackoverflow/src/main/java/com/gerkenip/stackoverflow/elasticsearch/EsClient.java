package com.gerkenip.stackoverflow.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequestBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.cluster.metadata.MappingMetaData;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import com.gerkenip.stackoverflow.elasticsearch.exception.EsDocumentDoesNotExistException;
import com.gerkenip.stackoverflow.elasticsearch.exception.EsException;
import com.gerkenip.stackoverflow.elasticsearch.exception.EsIndexDoesNotExistException;
import com.gerkenip.stackoverflow.elasticsearch.exception.EsServerException;
import com.gerkenip.stackoverflow.elasticsearch.exception.EsTypeNotDefinedException;

public class EsClient {

	private String host;
	private int port;
	private String cluster;
	
	private TransportClient _client = null;
	private BulkRequestBuilder bulkRequestBuilder;
	
	private ArrayList<String> bulkInserts = new ArrayList<String>();
	private int	bulkInsertMaxCount = 2000;
	private int bulkInsertMaxSize = 1000000;
	private int bulkInsertCount = 0;
	private int bulkInsertSize = 0;
	
	private ArrayList<InetSocketTransportAddress> otherHosts = new ArrayList<InetSocketTransportAddress>();

	public EsClient(String host, int port, String cluster) {
		this.host = host;
		this.port = port;
		this.cluster = cluster;
	}
	
	public static EsClient localClient() {
		return new EsClient("127.0.0.1", 9300, "elasticsearch");
	}
	
	public void addOtherHost(String host, int port) {
		otherHosts.add(new InetSocketTransportAddress(host, port));
	}
	
	public void close() {
		sendBulkInserts();
		if (_client != null) {
			_client.close();
		}
	}

	public void putDocument(String index, String type, String id, JSONObject document ) throws EsServerException {
		putDocument(index,type,id,document.toString());
	}
	
	public void putDocument(String index, String type, String id, String document ) throws EsServerException {
		
		if ((0 < bulkInsertCount) && (bulkInsertMaxSize <= (bulkInsertSize+document.length()))) {
			sendBulkInserts();
		}
		
		if (bulkRequestBuilder == null) {
			bulkRequestBuilder = getClient().prepareBulk();
		}
		
		IndexRequestBuilder indexRequestBuilder = getClient().prepareIndex(index,type,id).setSource(document);

		bulkRequestBuilder.add(indexRequestBuilder);
		bulkInsertCount++;
		bulkInsertSize = bulkInsertSize + document.length();
		
		bulkInserts.add(document);
		
		if ((bulkInsertCount >= bulkInsertMaxCount) || (bulkInsertMaxSize <= bulkInsertSize)) {
			sendBulkInserts();
		}
		
	}
	
	public String getDocument(String index, String type, String id) throws EsServerException, EsDocumentDoesNotExistException {
		
		GetResponse response = getClient().prepareGet(index, type, id).execute().actionGet();
		
		if (response.isExists()) {
			return response.getSourceAsString();
		}
		
		throw new EsDocumentDoesNotExistException(index, type, id);
	}
	
	public EsCursor getDocuments(String index, String queryString) {
		return new EsCursor(this, index, queryString);
	}
	

	public void createIndex(String indexName) throws EsServerException {		
		if (indexExists(indexName)) { return; }
		getClient().admin().indices().prepareCreate(indexName).execute().actionGet().isAcknowledged();
		reconnect();		
	}

	public void deleteIndex(String indexName) throws EsServerException {	
		if (!indexExists(indexName)) { return; }
		getClient().admin().indices().prepareDelete(indexName).execute().actionGet().isAcknowledged();
		reconnect();		
	}
	
	public String[] getIndexes() throws EsServerException {
		Map<String,IndexMetaData> mappings = getClient().admin().cluster().prepareState().execute().actionGet().getState().getMetaData().getIndices();
		Set<String> keys = mappings.keySet();
		String result[] = new String[keys.size()];
		keys.toArray(result);
		return result;
	}

	public boolean indexExists(String indexName) throws EsServerException {
		Map<String,IndexMetaData> mappings = getClient().admin().cluster().prepareState().execute().actionGet().getState().getMetaData().getIndices();
		return mappings.containsKey(indexName);
	}
	
	
	public void putMappingFromString(String index, String type, String mapping) throws EsServerException {
		IndicesAdminClient indicesAdminClient = getClient().admin().indices();
		PutMappingRequestBuilder pmrb = new PutMappingRequestBuilder(indicesAdminClient);
		pmrb.setIndices(index);
		pmrb.setType(type);
		pmrb.setSource(mapping);
		pmrb.execute().actionGet().isAcknowledged();
	}
	
	public String getMapping(String index, String type) throws EsException {
		IndexMetaData indexMetaData = getClient().admin().cluster().prepareState().setFilterIndices(index).execute().actionGet().getState().getMetaData().index(index);
		if (indexMetaData == null) {
			throw new EsIndexDoesNotExistException(index);
		}

		MappingMetaData mappingMetaData = indexMetaData.mapping(type);
		if (mappingMetaData == null) {
			throw new EsTypeNotDefinedException(index, type);
		}
		
		try {
			return mappingMetaData.source().string();
		} catch (IOException e) {
			throw new EsException(e);
		}

	}
	
	public boolean typeExists(String index, String type) throws EsIndexDoesNotExistException, EsServerException {
		ClusterState cs = getClient().admin().cluster().prepareState().setFilterIndices(index).execute().actionGet().getState();
		IndexMetaData imd = cs.getMetaData().index(index);
		if (imd == null) {
			throw new EsIndexDoesNotExistException(index);
		}	
		return imd.getMappings().containsKey(type);		
	}

	public void deleteType(String index, String type) throws EsServerException {
		try {
			if (!indexExists(index)) { 
				return;
			}
			if (!typeExists(index, type)) {
				return;
			}
		} catch (EsIndexDoesNotExistException e) {
			return;
		}
		getClient().admin().indices().prepareDeleteMapping(index).setType(type).execute().actionGet();
	}
	
	protected SearchResponse performQuery(JSONObject queryObj, String index) {
		SearchRequestBuilder srb = _client.prepareSearch(index);
		srb.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		srb.setSource(queryObj.toString());
		return srb.execute().actionGet();
	}
	
	
	
	private TransportClient getClient() throws EsServerException {
		if (_client == null) {
			try {
				connect();
				_client.admin().cluster().prepareState().execute();
			} catch (NoNodeAvailableException e) {
				throw new EsServerException();
			} catch (Throwable t) {
				System.out.println(t);
			}
		}
		return _client;
	}
	
	private void connect() {
		Builder builder = ImmutableSettings.settingsBuilder();
		builder.put("cluster.name", cluster);
		builder.put("client.transport.ping_timeout", "30s");

		TransportClient aClient = new TransportClient(builder.build());
		aClient.addTransportAddress(new InetSocketTransportAddress(host, port));	
		Iterator<InetSocketTransportAddress> iter = otherHosts.iterator();
		while (iter.hasNext()) {
			aClient.addTransportAddress(iter.next());	
		}
		
		_client = aClient;
	}

	private void reconnect() throws EsServerException {
		if (_client != null) {
			_client.close();
			_client = null;
			bulkRequestBuilder = null;
		}
		getClient();
	}

	public void sendBulkInserts() {
		
		if (bulkRequestBuilder == null) { return; }
				
		bulkRequestBuilder.execute();
		bulkRequestBuilder = null;
		
		bulkInserts	= new ArrayList<String>();
		bulkInsertCount = 0;
		bulkInsertSize = 0;
	}

	
}
