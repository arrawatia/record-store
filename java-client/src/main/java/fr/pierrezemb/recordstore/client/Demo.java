package fr.pierrezemb.recordstore.client;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.InvalidProtocolBufferException;
import fr.pierrezemb.recordstore.Constants;
import fr.pierrezemb.recordstore.datasets.proto.DemoUserProto;
import fr.pierrezemb.recordstore.proto.RecordStoreProtocol;

import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class Demo {
  public static void main(String[] args) throws ExecutionException, InterruptedException, TimeoutException, InvalidProtocolBufferException {
    RecordStoreClient recordStoreClient = new RecordStoreClient.Builder()
      .withTenant("demo")
      .withRecordSpace("user-1")
      .withToken("CigIABIGdGVuYW50EgRkZW1vGhYKFAgEEgQIABAAEgQIABAHEgQIABAIGiAOpNVQ7vf4eRxAhngHYyyEWkQvp/J+TDYZHNG1Is36tQ==")
      .withAddress("localhost:" + 8080)
//      .withAddress("34.68.182.220:80")
      .connect();

    RecordStoreProtocol.UpsertSchemaRequest request =
      SchemaUtils.createSchemaRequest(
        // descriptor generated by Protobuf
        DemoUserProto.User.getDescriptor(),
        // name of the RecordType
        DemoUserProto.User.class.getSimpleName(),
        // primary key field
        "id",
        // field to index
        "name",
        // how the field should be indexed
        RecordStoreProtocol.IndexType.VALUE
      );

    ListenableFuture<RecordStoreProtocol.EmptyResponse> resp = recordStoreClient.upsertSchema(request);
    System.out.println(resp.get());

    RecordStoreProtocol.GetSchemaRequest getSchemaRequest =
      RecordStoreProtocol.GetSchemaRequest.newBuilder().
        setRecordTypeName(DemoUserProto.User.class.getSimpleName()).build();
    ListenableFuture<RecordStoreProtocol.GetSchemaResponse> respgsr = recordStoreClient.getSchema(getSchemaRequest);
    System.out.println(respgsr.get().getSchemas());


    DemoUserProto.User record =
      DemoUserProto.User.newBuilder()
        .setId(999)
        .setName("Pierre Zemb")
        .setEmail("pz@example.org")
        .build();

    recordStoreClient.putRecord(record).get();

    RecordStoreProtocol.QueryRequest qrequest =
      RecordStoreProtocol.QueryRequest.newBuilder()
        // name of the RecordType to query
        .setRecordTypeName(DemoUserProto.User.class.getSimpleName())
        // retrieve only users with an id lower than 1000
        .setFilter(RecordQuery.field("id").lessThan(1000L))
        .build();

    Iterator<RecordStoreProtocol.QueryResponse> results =
      recordStoreClient.queryRecords(qrequest);

    for (Iterator<RecordStoreProtocol.QueryResponse> it = results; it.hasNext(); ) {
      RecordStoreProtocol.QueryResponse r = it.next();
      DemoUserProto.User response = DemoUserProto.User.parseFrom(r.getRecord().toByteArray());
      System.out.println(response);
    }
  }
}