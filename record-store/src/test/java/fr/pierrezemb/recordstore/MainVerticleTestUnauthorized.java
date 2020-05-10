package fr.pierrezemb.recordstore;

import com.google.protobuf.DescriptorProtos;
import fr.pierrezemb.recordstore.auth.BiscuitClientCredential;
import fr.pierrezemb.recordstore.auth.BiscuitManager;
import fr.pierrezemb.recordstore.proto.*;
import fr.pierrezemb.recordstore.utils.ProtobufReflectionUtil;
import io.grpc.ManagedChannel;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.grpc.VertxChannelBuilder;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MainVerticleTestUnauthorized {

  public static final String DEFAULT_TENANT = "my-tenant";
  public static final String DEFAULT_CONTAINER = "my-container";
  public final int port = PortManager.nextFreePort();
  private final FoundationDBContainer container = new FoundationDBContainer();
  private AdminServiceGrpc.AdminServiceVertxStub adminServiceVertxStub;
  private File clusterFile;

  @BeforeAll
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) throws IOException, InterruptedException {

    container.start();
    clusterFile = container.getClusterFile();

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject()
        .put("fdb-cluster-file", clusterFile.getAbsolutePath())
        .put("listen-port", port));

    BiscuitManager biscuitManager = new BiscuitManager();
    String sealedBiscuit = biscuitManager.create(DEFAULT_TENANT, Collections.emptyList());
    BiscuitClientCredential credentials = new BiscuitClientCredential(DEFAULT_TENANT + "dsa", sealedBiscuit, DEFAULT_CONTAINER);

    // deploy verticle
    vertx.deployVerticle(new MainVerticle(), options, testContext.succeeding(id -> testContext.completeNow()));
    ManagedChannel channel = VertxChannelBuilder
      .forAddress(vertx, "localhost", port)
      .usePlaintext(true)
      .build();

    adminServiceVertxStub = AdminServiceGrpc.newVertxStub(channel).withCallCredentials(credentials);
  }

  @Test
  public void testBadAuth(Vertx vertx, VertxTestContext testContext) throws Exception {

    adminServiceVertxStub.ping(RecordStoreProtocol.EmptyRequest.newBuilder().build(), response -> {
      if (response.succeeded()) {
        testContext.failNow(response.cause());
      } else {
        testContext.completeNow();
      }
    });
  }

}
