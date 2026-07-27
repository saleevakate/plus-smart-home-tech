package ru.yandex.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.collector.handler.hub.HubEventHandler;
import ru.yandex.practicum.collector.handler.sensor.SensorEventHandler;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.service.collector.CollectorControllerGrpc;


import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
public class EventGrpcController extends CollectorControllerGrpc.CollectorControllerImplBase {

    private final Map<SensorEventProto.PayloadCase, SensorEventHandler> sensorEventHandlers;
    private final Map<HubEventProto.PayloadCase, HubEventHandler> hubEventHandlers;

    public EventGrpcController(Set<SensorEventHandler> sensorEventHandlers,
                               Set<HubEventHandler> hubEventHandlers) {
        this.sensorEventHandlers = sensorEventHandlers.stream()
                .collect(Collectors.toMap(
                        SensorEventHandler::getMessageType,
                        Function.identity()
                ));
        this.hubEventHandlers = hubEventHandlers.stream()
                .collect(Collectors.toMap(
                        HubEventHandler::getMessageType,
                        Function.identity()
                ));
    }

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Получено событие датчика через gRPC: type={}, id={}, hubId={}",
                    request.getPayloadCase(), request.getId(), request.getHubId());

            SensorEventHandler handler = sensorEventHandlers.get(request.getPayloadCase());

            if (handler == null) {
                String errorMsg = "Не найден обработчик для типа датчика: " + request.getPayloadCase();
                log.warn(errorMsg);
                responseObserver.onError(new StatusRuntimeException(
                        Status.INVALID_ARGUMENT.withDescription(errorMsg)
                ));
                return;
            }

            handler.handle(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка при обработке события датчика", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.info("Получено событие хаба через gRPC: type={}, hubId={}",
                    request.getPayloadCase(), request.getHubId());

            HubEventHandler handler = hubEventHandlers.get(request.getPayloadCase());

            if (handler == null) {
                String errorMsg = "Не найден обработчик для типа события хаба: " + request.getPayloadCase();
                log.warn(errorMsg);
                responseObserver.onError(new StatusRuntimeException(
                        Status.INVALID_ARGUMENT.withDescription(errorMsg)
                ));
                return;
            }

            handler.handle(request);

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Ошибка при обработке события хаба", e);
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}
