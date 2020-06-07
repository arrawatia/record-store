package fr.pierrezemb.recordstore.fdb;

import com.apple.foundationdb.record.provider.foundationdb.FDBMetaDataStore;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordContext;

public class RecordStoreMetaDataStore {
  public static FDBMetaDataStore createMetadataStore(FDBRecordContext context, String tenant, String env) {
    FDBMetaDataStore metaDataStore = new FDBMetaDataStore(context, RecordStoreKeySpace.getMetaDataKeySpacePath(tenant, env));
    metaDataStore.setMaintainHistory(true);
    return metaDataStore;
  }
}