package fr.pierrezemb.recordstore.grpc;

import fr.pierrezemb.recordstore.fdb.RecordLayer;
import fr.pierrezemb.recordstore.proto.AdminServiceGrpc;
import fr.pierrezemb.recordstore.proto.RecordStoreProtocol;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AdminService extends AdminServiceGrpc.AdminServiceImplBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class);
  private final RecordLayer recordLayer;

  public AdminService(RecordLayer recordLayer) {
    this.recordLayer = recordLayer;

  }

  /**
   * @param request
   * @param responseObserver
   */
  @Override
  public void list(RecordStoreProtocol.ListContainerRequest request, StreamObserver<RecordStoreProtocol.ListContainerResponse> responseObserver) {
    String tenantID = GrpcContextKeys.getTenantIDOrFail();

    List<String> results;
    try {
      results = recordLayer.listContainers(tenantID);
    } catch (RuntimeException e) {
      LOGGER.error("cannot list containers: {}", e);
      throw new StatusRuntimeException(Status.INTERNAL.withCause(e));
    }

    responseObserver.onNext(RecordStoreProtocol.ListContainerResponse.newBuilder()
      .addAllContainers(results)
      .build());
    responseObserver.onCompleted();
  }

  /**
   * @param request
   * @param responseObserver
   */
  @Override
  public void delete(RecordStoreProtocol.DeleteContainerRequest request, StreamObserver<RecordStoreProtocol.EmptyResponse> responseObserver) {
    String tenantID = GrpcContextKeys.getTenantIDOrFail();

    try {
      for (String container : request.getContainersList()) {
        recordLayer.deleteContainer(tenantID, container);
      }
    } catch (RuntimeException runtimeException) {
      LOGGER.error("could not delete container", runtimeException);
      throw new StatusRuntimeException(Status.INTERNAL.withDescription(runtimeException.getMessage()));
    }

    responseObserver.onNext(RecordStoreProtocol.EmptyResponse.newBuilder()
      .build());
    responseObserver.onCompleted();
  }

  @Override
  public void ping(RecordStoreProtocol.EmptyRequest request, StreamObserver<RecordStoreProtocol.EmptyResponse> responseObserver) {
    GrpcContextKeys.getTenantIDOrFail();
    responseObserver.onNext(RecordStoreProtocol.EmptyResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}