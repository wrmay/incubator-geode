/*=========================================================================
 * Copyright (c) 2010-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 *=========================================================================
 */
package com.gemstone.gemfire.management.internal.cli.functions;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Declarable;
import com.gemstone.gemfire.cache.execute.FunctionAdapter;
import com.gemstone.gemfire.cache.execute.FunctionContext;
import com.gemstone.gemfire.cache.server.CacheServer;
import com.gemstone.gemfire.distributed.internal.DistributionConfig;
import com.gemstone.gemfire.distributed.internal.DistributionConfigImpl;
import com.gemstone.gemfire.distributed.internal.InternalDistributedSystem;
import com.gemstone.gemfire.internal.ConfigSource;
import com.gemstone.gemfire.internal.InternalEntity;
import com.gemstone.gemfire.internal.cache.CacheConfig;
import com.gemstone.gemfire.internal.cache.GemFireCacheImpl;
import com.gemstone.gemfire.internal.cache.ha.HARegionQueue;
import com.gemstone.gemfire.management.internal.cli.domain.MemberConfigurationInfo;

/****
 * 
 * @author bansods
 *
 */
public class GetMemberConfigInformationFunction extends FunctionAdapter implements InternalEntity {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;


  @Override
  public void execute(FunctionContext context) {
    Object argsObject = context.getArguments();
    boolean hideDefaults = ((Boolean)argsObject).booleanValue();
    
    Cache cache = CacheFactory.getAnyInstance();
    InternalDistributedSystem system = (InternalDistributedSystem) cache.getDistributedSystem();
    DistributionConfig  config = system.getConfig();
    
    DistributionConfigImpl distConfigImpl = ((DistributionConfigImpl) config);
    MemberConfigurationInfo memberConfigInfo = new MemberConfigurationInfo();
    memberConfigInfo.setJvmInputArguments(getJvmInputArguments()); 
    memberConfigInfo.setGfePropsRuntime(distConfigImpl.getConfigPropsFromSource(ConfigSource.runtime()));
    memberConfigInfo.setGfePropsSetUsingApi(distConfigImpl.getConfigPropsFromSource(ConfigSource.api()));
    
    if (!hideDefaults)
      memberConfigInfo.setGfePropsSetWithDefaults(distConfigImpl.getConfigPropsFromSource(null));
    
    memberConfigInfo.setGfePropsSetFromFile(distConfigImpl.getConfigPropsDefinedUsingFiles());
    
    //CacheAttributes
    Map<String, String> cacheAttributes = new HashMap<String, String>();

    cacheAttributes.put("copy-on-read", Boolean.toString(cache.getCopyOnRead()));
    cacheAttributes.put("is-server", Boolean.toString(cache.isServer()));
    cacheAttributes.put("lock-timeout", Integer.toString(cache.getLockTimeout()));
    cacheAttributes.put("lock-lease", Integer.toString(cache.getLockLease()));
    cacheAttributes.put("message-sync-interval", Integer.toString(cache.getMessageSyncInterval()));
    cacheAttributes.put("search-timeout", Integer.toString(cache.getSearchTimeout()));
    
    if (cache.getPdxDiskStore() == null) {
      cacheAttributes.put("pdx-disk-store", "");
    }
    else {
      cacheAttributes.put("pdx-disk-store", cache.getPdxDiskStore());
    }
    
    cacheAttributes.put("pdx-ignore-unread-fields", Boolean.toString(cache.getPdxIgnoreUnreadFields()));
    cacheAttributes.put("pdx-persistent", Boolean.toString(cache.getPdxPersistent()));
    cacheAttributes.put("pdx-read-serialized", Boolean.toString(cache.getPdxReadSerialized()));
    
    if (hideDefaults) {
      removeDefaults(cacheAttributes, getCacheAttributesDefaultValues());
    }
    
    memberConfigInfo.setCacheAttributes(cacheAttributes);
  
    List<Map<String, String>> cacheServerAttributesList = new ArrayList<Map<String, String>>();
    List<CacheServer> cacheServers = cache.getCacheServers();
    
    if (cacheServers != null)
      for (CacheServer cacheServer : cacheServers) {
        Map<String, String> cacheServerAttributes = new HashMap<String, String>();

        cacheServerAttributes.put("bind-address", cacheServer.getBindAddress());
        cacheServerAttributes.put("hostname-for-clients", cacheServer.getHostnameForClients());
        cacheServerAttributes.put("max-connections", Integer.toString(cacheServer.getMaxConnections()));
        cacheServerAttributes.put("maximum-message-count", Integer.toString(cacheServer.getMaximumMessageCount()));
        cacheServerAttributes.put("maximum-time-between-pings", Integer.toString(cacheServer.getMaximumTimeBetweenPings()));
        cacheServerAttributes.put("max-threads", Integer.toString(cacheServer.getMaxThreads()));
        cacheServerAttributes.put("message-time-to-live", Integer.toString(cacheServer.getMessageTimeToLive()));
        cacheServerAttributes.put("notify-by-subscription", Boolean.toString(cacheServer.getNotifyBySubscription()));
        cacheServerAttributes.put("port", Integer.toString(cacheServer.getPort()));
        cacheServerAttributes.put("socket-buffer-size", Integer.toString(cacheServer.getSocketBufferSize()));
        cacheServerAttributes.put("load-poll-interval", Long.toString(cacheServer.getLoadPollInterval()));
        cacheServerAttributes.put("tcp-no-delay", Boolean.toString(cacheServer.getTcpNoDelay()));

        if (hideDefaults)
          removeDefaults(cacheServerAttributes, getCacheServerAttributesDefaultValues());
        
        cacheServerAttributesList.add(cacheServerAttributes);
    }
    
    memberConfigInfo.setCacheServerAttributes(cacheServerAttributesList);
  
    context.getResultSender().lastResult(memberConfigInfo);
  }
  /****
   * Gets the default values for the cache attributes
   * @return a map containing the cache attributes - default values
   */
  private Map<String, String> getCacheAttributesDefaultValues() {
    String d = CacheConfig.DEFAULT_PDX_DISK_STORE;
    Map <String, String> cacheAttributesDefault = new HashMap<String, String> ();
    cacheAttributesDefault.put("pdx-disk-store", "");
    cacheAttributesDefault.put("pdx-read-serialized", Boolean.toString(CacheConfig.DEFAULT_PDX_READ_SERIALIZED));
    cacheAttributesDefault.put("pdx-ignore-unread-fields", Boolean.toString(CacheConfig.DEFAULT_PDX_IGNORE_UNREAD_FIELDS));
    cacheAttributesDefault.put("pdx-persistent", Boolean.toString(CacheConfig.DEFAULT_PDX_PERSISTENT));
    cacheAttributesDefault.put("copy-on-read", Boolean.toString(GemFireCacheImpl.DEFAULT_COPY_ON_READ));
    cacheAttributesDefault.put("lock-timeout", Integer.toString(GemFireCacheImpl.DEFAULT_LOCK_TIMEOUT));
    cacheAttributesDefault.put("lock-lease", Integer.toString(GemFireCacheImpl.DEFAULT_LOCK_LEASE));
    cacheAttributesDefault.put("message-sync-interval", Integer.toString(HARegionQueue.DEFAULT_MESSAGE_SYNC_INTERVAL));
    cacheAttributesDefault.put("search-timeout", Integer.toString(GemFireCacheImpl.DEFAULT_SEARCH_TIMEOUT));
    cacheAttributesDefault.put("is-server", Boolean.toString(false));

    return cacheAttributesDefault;
  }
  
  /***
   * Gets the default values for the cache attributes
   * @return a map containing the cache server attributes - default values
   */
  private Map<String, String> getCacheServerAttributesDefaultValues() {
    Map <String, String> csAttributesDefault = new HashMap<String, String> ();
    csAttributesDefault.put("bind-address", CacheServer.DEFAULT_BIND_ADDRESS);
    csAttributesDefault.put("hostname-for-clients", CacheServer.DEFAULT_HOSTNAME_FOR_CLIENTS);
    csAttributesDefault.put("max-connections", Integer.toString(CacheServer.DEFAULT_MAX_CONNECTIONS));
    csAttributesDefault.put("maximum-message-count", Integer.toString(CacheServer.DEFAULT_MAXIMUM_MESSAGE_COUNT));
    csAttributesDefault.put("maximum-time-between-pings", Integer.toString(CacheServer.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS));
    csAttributesDefault.put("max-threads", Integer.toString(CacheServer.DEFAULT_MAX_THREADS));
    csAttributesDefault.put("message-time-to-live", Integer.toString(CacheServer.DEFAULT_MESSAGE_TIME_TO_LIVE));
    csAttributesDefault.put("notify-by-subscription", Boolean.toString(CacheServer.DEFAULT_NOTIFY_BY_SUBSCRIPTION));
    csAttributesDefault.put("port", Integer.toString(CacheServer.DEFAULT_PORT));
    csAttributesDefault.put("socket-buffer-size", Integer.toString(CacheServer.DEFAULT_SOCKET_BUFFER_SIZE));
    csAttributesDefault.put("load-poll-interval", Long.toString(CacheServer.DEFAULT_LOAD_POLL_INTERVAL));
    return csAttributesDefault;

  }
  
  /****
   * Removes the default values from the attributesMap based on defaultAttributesMap
   * @param attributesMap
   * @param defaultAttributesMap
   */
  private void removeDefaults (Map<String, String> attributesMap, Map<String, String> defaultAttributesMap) {
    //Make a copy to avoid the CME's
    Set<String> attributesSet = new HashSet<String>(attributesMap.keySet());
    
    if (attributesSet != null) {
      for (String attribute : attributesSet) {
        String attributeValue = attributesMap.get(attribute);
        String defaultValue = defaultAttributesMap.get(attribute);
        
        if (attributeValue != null) {
          if (attributeValue.equals(defaultValue)) {
            attributesMap.remove(attribute);
          }
        } else {
          if (defaultValue == null || defaultValue.equals("")) {
            attributesMap.remove(attribute);
          }
        }
      }
    }
  }
  @Override
  public String getId() {
    // TODO Auto-generated method stub
    return GetMemberConfigInformationFunction.class.toString();
  }
  
  private List<String> getJvmInputArguments() {
    RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    return runtimeBean.getInputArguments();
  }
}
