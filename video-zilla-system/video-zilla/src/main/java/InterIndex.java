import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedList;
import java.time.Duration;
import com.telmomenezes.jfastemd.*;
import com.hierarchy.index.*;

public class InterIndex extends AbstractBehavior<InterIndex.Command> {
    public interface Command {}

    // setup msg
    private class CameraTerminated implements Command {
        public final ActorRef<IntraIndex.Command> intraIndex;
        public final String appId;
        public final String deviceId;

        CameraTerminated(ActorRef<IntraIndex.Command> intraIndex, String appId, String deviceId) {
            this.intraIndex = intraIndex;
            this.appId = appId;
            this.deviceId = deviceId;
        }
    }

    public static class RespondFeatures implements Command {
        final String deviceId;
        final List<List<Signature>> response;

        public RespondFeatures(String deviceId, List<List<Signature>> response) {
            this.deviceId = deviceId;
            this.response = new LinkedList<>();
            for (List<Signature> list : response) {
                this.response.add(new LinkedList<>(list));
            }
        }
    }

    public static Behavior<Command> create(String appId) {
        return Behaviors.setup(context -> new InterIndex(context, appId));
    }
    
    private final String appId;
    private final Map<String, ActorRef<IntraIndex.Command>> deviceIdToActor = new HashMap<>();
    private InterCameraIndex inter;
    
    private InterIndex(ActorContext<Command> context, String appId) {
        super(context);
        this.appId = appId;
        inter = new InterCameraIndex(appId);
        context.getLog().info("InterIndex {} started", appId);
    }
    
    // Event-triggered functions
    // setup functions
    private InterIndex onTrackCamera(IndexSupervisor.RequestTrackCamera trackMsg) {
        if (this.appId.equals(trackMsg.appId)) {
            ActorRef<IntraIndex.Command> intraIndexActor = deviceIdToActor.get(trackMsg.deviceId);
            if (intraIndexActor != null) {
                trackMsg.replyTo.tell(new IndexSupervisor.CameraRegistered(intraIndexActor));
            } else {
                getContext().getLog().info("Creating intraindex actor for camera {}", trackMsg.deviceId);
                intraIndexActor = 
                    getContext()
                        .spawn(
                            IntraIndex.create(appId, trackMsg.deviceId, getContext().getSelf()), 
                            "intraindex-" + trackMsg.deviceId);
                getContext()
                    .watchWith(intraIndexActor, new CameraTerminated(intraIndexActor, appId, trackMsg.deviceId));
                deviceIdToActor.put(trackMsg.deviceId, intraIndexActor);
                trackMsg.replyTo.tell(new IndexSupervisor.CameraRegistered(intraIndexActor));
            }
        } else {
            getContext()
                .getLog()
                .warn(
                    "Ignoring TrackCamera request for app {}, this actor is responsible for app {}.",
                    appId,
                    this.appId);
        }
        return this;
    }

    private InterIndex onCameraList(IndexSupervisor.RequestCameraList r) {
        r.replyTo.tell(new IndexSupervisor.ReplyCameraList(r.requestId, deviceIdToActor.keySet()));
        return this;
    }

    private InterIndex onTerminated(CameraTerminated t) {
        getContext().getLog().info("IntraIndex actor for {} has been terminated", t.deviceId);
        deviceIdToActor.remove(t.deviceId);
        return this;
    }

    // query functions
    // cluster query
    private InterIndex onClusterQuery(IndexSupervisor.RequestSignaturesBySignature r) {
        Map<String, ActorRef<IntraIndex.Command>> deviceIdToCandidates = new HashMap<>();
        List<String> interRes = inter.clusterQuery(r.targetSignature);
        if (interRes == null || interRes.size() == 0) return this;
        deviceIdToCandidates.put(interRes.get(0), deviceIdToActor.get(interRes.get(0)));

        getContext()
            .spawnAnonymous(
                ClusterQuery.create(
                    deviceIdToCandidates, r.requestId, interRes.get(1), 
                    r.replyTo, Duration.ofSeconds(10)));
        return this;
    }

    // direct query
    private InterIndex onDirectQuery(IndexSupervisor.RequestSignaturesByFeature r) {
        Map<String, ActorRef<IntraIndex.Command>> deviceIdToCandidates = new HashMap<>();
        Map<String, Set<String>> interRes = inter.directQuery(r.targetFeature);
        for (String devId : interRes.keySet()) {
            deviceIdToCandidates.put(devId, deviceIdToActor.get(devId));
        }

        getContext()
            .spawnAnonymous(
                DirectQuery.create(
                    deviceIdToCandidates, r.requestId, r.targetFeature, interRes, 
                    r.replyTo, Duration.ofSeconds(10)));
        return this;
    }

    private InterIndex onRespondFeatures(RespondFeatures r) {
        getContext()
            .getLog()
            .info("Update inter index for app {}, based on the changes in camera {}", 
                    appId, r.deviceId);
        inter.updateIndex(r.response, r.deviceId);
        return this;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(IndexSupervisor.RequestTrackCamera.class, this::onTrackCamera)
            .onMessage(CameraTerminated.class, this::onTerminated)
            .onMessage(RespondFeatures.class, this::onRespondFeatures)
            .onMessage(
                    IndexSupervisor.RequestCameraList.class,
                    r -> r.appId.equals(appId),
                    this::onCameraList)
            .onMessage(
                IndexSupervisor.RequestSignaturesBySignature.class,
                r -> r.appId.equals(appId),
                this::onClusterQuery)
            .onMessage(
                IndexSupervisor.RequestSignaturesByFeature.class,
                r -> r.appId.equals(appId),
                this::onDirectQuery)
            .onSignal(PostStop.class, signal -> onPostStop())
            .build();
    }

    private InterIndex onPostStop() {
        getContext().getLog().info("InterIndex {} stopped", appId);
        return this;
    }
}
