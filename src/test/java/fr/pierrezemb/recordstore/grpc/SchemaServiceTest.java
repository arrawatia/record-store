package fr.pierrezemb.recordstore.grpc;

import com.google.protobuf.DescriptorProtos;
import fr.pierrezemb.recordstore.FoundationDBContainer;
import fr.pierrezemb.recordstore.MainVerticle;
import fr.pierrezemb.recordstore.proto.AdminServiceGrpc;
import fr.pierrezemb.recordstore.proto.RecordStoreProtocol;
import fr.pierrezemb.recordstore.proto.RecordStoreProtocolTest;
import fr.pierrezemb.recordstore.proto.SchemaServiceGrpc;
import fr.pierrezemb.recordstore.utils.ProtobufReflectionUtil;
import io.grpc.ManagedChannel;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SchemaServiceTest {

  private final FoundationDBContainer container = new FoundationDBContainer();
  private SchemaServiceGrpc.SchemaServiceVertxStub schemaServiceVertxStub;
  private AdminServiceGrpc.AdminServiceVertxStub adminServiceVertxStub;
  private File clusterFile;

  @BeforeAll
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) throws IOException, InterruptedException {

    container.start();
    clusterFile = container.getClusterFile();

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject().put("fdb-cluster-file", clusterFile.getAbsolutePath())
      );

    // deploy verticle
    vertx.deployVerticle(new MainVerticle(), options, testContext.succeeding(id -> testContext.completeNow()));
    ManagedChannel channel = VertxChannelBuilder
      .forAddress(vertx, "localhost", 8080)
      .usePlaintext(true)
      .build();

    schemaServiceVertxStub = SchemaServiceGrpc.newVertxStub(channel);
    adminServiceVertxStub = AdminServiceGrpc.newVertxStub(channel);
  }

  @Test
  public void testCRUDSchema1(Vertx vertx, VertxTestContext testContext) throws Exception {

    DescriptorProtos.FileDescriptorSet dependencies =
      ProtobufReflectionUtil.protoFileDescriptorSet(RecordStoreProtocolTest.Person.getDescriptor());

    RecordStoreProtocol.SelfDescribedMessage selfDescribedMessage = RecordStoreProtocol.SelfDescribedMessage
      .newBuilder()
      .setDescriptorSet(dependencies)
      .build();

    RecordStoreProtocol.UpsertSchemaRequest request = RecordStoreProtocol.UpsertSchemaRequest
      .newBuilder()
      .setName("Person")
      .addPrimaryKeyFields("id")
      .setSchema(selfDescribedMessage)
      .build();

    schemaServiceVertxStub.upsert(request, response -> {
      if (response.succeeded()) {
        System.out.println("Got the server response: " + response.result().getResult());
        testContext.completeNow();
      } else {
        testContext.failNow(response.cause());
      }
    });
  }

  @Test
  public void testCRUDSchema2(Vertx vertx, VertxTestContext testContext) throws Exception {
    schemaServiceVertxStub.get(RecordStoreProtocol.GetSchemaRequest.newBuilder()
      .setTable("Person")
      .build(), response -> {
      if (response.succeeded()) {
        System.out.println("Got the server response: " + response.result());
        testContext.completeNow();
      } else {
        testContext.failNow(response.cause());
      }
    });
  }

  @Test
  public void testCRUDSchema3(Vertx vertx, VertxTestContext testContext) throws Exception {
    DescriptorProtos.FileDescriptorSet dependencies =
      ProtobufReflectionUtil.protoFileDescriptorSet(RecordStoreProtocolTest.Person.getDescriptor());

    RecordStoreProtocol.SelfDescribedMessage selfDescribedMessage = RecordStoreProtocol.SelfDescribedMessage
      .newBuilder()
      .setDescriptorSet(dependencies)
      .build();

    RecordStoreProtocol.UpsertSchemaRequest request = RecordStoreProtocol.UpsertSchemaRequest
      .newBuilder()
      .setName("Person")
      .addPrimaryKeyFields("id")
      .addIndexDefinitions(RecordStoreProtocol.IndexDefinition.newBuilder()
        .setField("name")
        .setIndexType(RecordStoreProtocol.IndexType.VALUE)
        .build())
      .setSchema(selfDescribedMessage)
      .build();

    schemaServiceVertxStub.upsert(request, response -> {
      if (response.succeeded()) {
        System.out.println("Got the server response: " + response.result().getResult());
        testContext.completeNow();
      } else {
        testContext.failNow(response.cause());
      }
    });
  }

  @Test
  public void testCRUDSchema4(Vertx vertx, VertxTestContext testContext) throws Exception {

    DescriptorProtos.FileDescriptorSet dependencies =
      ProtobufReflectionUtil.protoFileDescriptorSet(RecordStoreProtocolTest.Person.getDescriptor());

    RecordStoreProtocol.SelfDescribedMessage selfDescribedMessage = RecordStoreProtocol.SelfDescribedMessage
      .newBuilder()
      .setDescriptorSet(dependencies)
      .build();

    RecordStoreProtocol.UpsertSchemaRequest request = RecordStoreProtocol.UpsertSchemaRequest
      .newBuilder()
      .setName("Person")
      .addPrimaryKeyFields("id")
      // let's forget an index
      .setSchema(selfDescribedMessage)
      .build();

    schemaServiceVertxStub.upsert(request, response -> {
      if (response.succeeded()) {
        testContext.failNow(new Throwable("should have failed"));
      } else {
        testContext.completeNow();
      }
    });
  }

  @Test
  public void testCRUDSchema6(Vertx vertx, VertxTestContext testContext) throws Exception {
    adminServiceVertxStub.deleteAll(RecordStoreProtocol.DeleteAllRequest.newBuilder()
      .build(), response -> {
      if (response.succeeded()) {
        System.out.println("Got the server response: " + response.result().getResult());
        testContext.completeNow();
      } else {
        testContext.failNow(response.cause());
      }
    });
  }
}
