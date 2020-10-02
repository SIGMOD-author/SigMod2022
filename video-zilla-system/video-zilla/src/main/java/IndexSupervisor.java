import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.telmomenezes.jfastemd.*;

public class IndexSupervisor extends AbstractBehavior<IndexSupervisor.Command> {

    public interface Command {}

    // setup msg
    public static final class RequestTrackCamera
            implements IndexSupervisor.Command, InterIndex.Command {
        public final String appId;
        public final String deviceId;
        public final ActorRef<Command> replyTo;

        public RequestTrackCamera(String appId, String deviceId, ActorRef<Command> replyTo) {
            this.appId = appId;
            this.deviceId = deviceId;
            this.replyTo = replyTo;
        }
    }

    public static final class UserTrackCamera
            implements Command {
        public final String appId;
        public final String deviceId;

        public UserTrackCamera(String appId, String deviceId) {
            this.appId = appId;
            this.deviceId = deviceId;
        }
    }

    public static final class CameraRegistered implements Command {
        public final ActorRef<IntraIndex.Command> intraIndex;

        public CameraRegistered(ActorRef<IntraIndex.Command> intraIndex) {
            this.intraIndex = intraIndex;
        }
    }

    public static final class RequestCameraList
            implements IndexSupervisor.Command, InterIndex.Command {
        final long requestId;
        final String appId;
        final ActorRef<ReplyCameraList> replyTo;

        public RequestCameraList(long requestId, String appId, ActorRef<ReplyCameraList> replyTo) {
            this.requestId = requestId;
            this.appId = appId;
            this.replyTo = replyTo;
        }
    }

    public static final class ReplyCameraList {
        final long requestId;
        final Set<String> ids;

        public ReplyCameraList(long requestId, Set<String> ids) {
            this.requestId = requestId;
            this.ids = ids;
        }
    }

    private static class InterIndexTerminated implements IndexSupervisor.Command {
        public final String appId;

        InterIndexTerminated(String appId) {
            this.appId = appId;
        }
    }
    // query msg
    // cluster query msg
    public static final class RequestSignaturesBySignature
            implements ClusterQuery.Command, IndexSupervisor.Command, InterIndex.Command {
        final long requestId;
        final String appId;
        final ActorRef<Command> replyTo;
        final Signature targetSignature;

        public RequestSignaturesBySignature(
            long requestId, String appId, 
            ActorRef<Command> replyTo, Signature targetSignature) {
            this.requestId = requestId;
            this.appId = appId;
            this.replyTo = replyTo;
            this.targetSignature = targetSignature;
        }
    }

    public static final class UserRequestSignaturesBySignature
            implements Command {
        final long requestId;
        final String appId;
        final Signature targetSignature;

        public UserRequestSignaturesBySignature(
            long requestId, String appId, 
            Signature targetSignature) {
            this.requestId = requestId;
            this.appId = appId;
            this.targetSignature = targetSignature;
        }
    }

    // direct query msg
    public static final class RequestSignaturesByFeature
            implements DirectQuery.Command, IndexSupervisor.Command, InterIndex.Command {
        final long requestId;
        final String appId;
        final ActorRef<Command> replyTo;
        final FeatureND targetFeature;

        public RequestSignaturesByFeature(
            long requestId, String appId, 
            ActorRef<Command> replyTo, FeatureND targetFeature) {
            this.requestId = requestId;
            this.appId = appId;
            this.replyTo = replyTo;
            this.targetFeature = targetFeature;
        }
    }

    public static final class UserRequestSignaturesByFeature
            implements Command {
        final long requestId;
        final String appId;
        final FeatureND targetFeature;

        public UserRequestSignaturesByFeature (
            long requestId, String appId, 
            FeatureND targetFeature) {
            this.requestId = requestId;
            this.appId = appId;
            this.targetFeature = targetFeature;
        }
    }

    public static final class RespondAllSignatures implements Command {
        final long requestId;
        final Map<String, SignatureResult> signatures;
        
        public RespondAllSignatures(long requestId, Map<String, SignatureResult> signatures) {
            this.requestId = requestId;
            this.signatures = signatures;
        }
    }

    // query result format
    public interface SignatureResult {}

    public static final class SignatureList implements SignatureResult {
        public final List<Signature> list;
        
        public SignatureList(List<Signature> list) {
            this.list = list;
        }
    } 

    public enum CameraNotAvailable implements SignatureResult {
        INSTANCE
    } 

    public enum CameraTimeOut implements SignatureResult {
        INSTANCE
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(IndexSupervisor::new);
    }

    private final Map<String, ActorRef<InterIndex.Command>> appIdToActor = new HashMap<>();

    private IndexSupervisor(ActorContext<Command> context) {
        super(context);
        context.getLog().info("Index supervisor started");
    }

    // Even-triggered function
    // setup functions
    private IndexSupervisor onUserTrackCamera(UserTrackCamera trackMsg) {
        String appId = trackMsg.appId;
        ActorRef<InterIndex.Command> ref = appIdToActor.get(appId);
        if (ref != null) {
            ref.tell(new RequestTrackCamera(trackMsg.appId, trackMsg.deviceId, getContext().getSelf()));
        } else {
            getContext().getLog().info("Creating Inter Index actor for app {}", appId);
            ActorRef<InterIndex.Command> interIndexActor = 
                getContext().spawn(InterIndex.create(appId), "app-" + appId);
            getContext().watchWith(interIndexActor, new InterIndexTerminated(appId));
            interIndexActor.tell(new RequestTrackCamera(trackMsg.appId, trackMsg.deviceId, getContext().getSelf()));
            //interIndexActor.tell(trackMsg);
            appIdToActor.put(appId, interIndexActor);
        }
        return this;
    }

    private IndexSupervisor onCameraRegistered(CameraRegistered cr) {
        getContext().getLog().info("Done with registration!");
        cr.intraIndex.tell(new IntraIndex.ReadFeatures(1));
        return this;
    }

    private IndexSupervisor onRequestCameraList(RequestCameraList request) {
        ActorRef<InterIndex.Command> ref = appIdToActor.get(request.appId);
        if (ref != null) {
            ref.tell(request);
        } else {
            request.replyTo.tell(new ReplyCameraList(request.requestId, Collections.emptySet()));
        }
        return this;
    } 

    private IndexSupervisor onTerminated(InterIndexTerminated t) {
        getContext().getLog().info("Inter index actor for app {} has been terminated", t.appId);
        appIdToActor.remove(t.appId);
        return this;
    }

    // query functions
    // Cluster query
    private IndexSupervisor onUserRequestSignaturesBySignature(
            UserRequestSignaturesBySignature request) {
        ActorRef<InterIndex.Command> ref = appIdToActor.get(request.appId);
        if (ref != null) {
            ref.tell(new RequestSignaturesBySignature(
                        request.requestId, request.appId, 
                        getContext().getSelf(), request.targetSignature));
        } else {
            getContext().getSelf().tell(new RespondAllSignatures(request.requestId, Collections.emptyMap()));
        }
        return this;
    } 

    // Direct query
    private IndexSupervisor onUserRequestSignaturesByFeature(
            UserRequestSignaturesByFeature request) {
        ActorRef<InterIndex.Command> ref = appIdToActor.get(request.appId);
        if (ref != null) {
            ref.tell(new RequestSignaturesByFeature(
                        request.requestId, request.appId, 
                        getContext().getSelf(), request.targetFeature));
        } else {
            getContext().getSelf().tell(new RespondAllSignatures(request.requestId, Collections.emptyMap()));
        }
        return this;
    } 

    // Process query results
    private IndexSupervisor onRespondAllSignatures(RespondAllSignatures response) {
        if (response.signatures.size() != 0) {
            for (SignatureResult sr : response.signatures.values()) {
                if (sr instanceof SignatureList) {
                    SignatureList tmp = (SignatureList)sr;
                    for (Signature s : tmp.list) {
                        getContext().getLog().info(s.getLabel());
                    } 
                }
            }
        }
        return this;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            // camera register
            .onMessage(UserTrackCamera.class, this::onUserTrackCamera)
            .onMessage(CameraRegistered.class, this::onCameraRegistered)
            // terminate an application-specific hierarchical index
            .onMessage(InterIndexTerminated.class, this::onTerminated)
            // get list of cameras
            .onMessage(RequestCameraList.class, this::onRequestCameraList)
            // actual queries
            .onMessage(UserRequestSignaturesBySignature.class, 
                    this::onUserRequestSignaturesBySignature)
            .onMessage(UserRequestSignaturesByFeature.class, 
                    this::onUserRequestSignaturesByFeature)
            .onMessage(RespondAllSignatures.class,
                    this::onRespondAllSignatures)
            // Post stop logging
            .onSignal(PostStop.class, signal -> onPostStop())
            .build();
    }

    private IndexSupervisor onPostStop() {
        getContext().getLog().info("Video-zilla stopped");
        return this;
    }
}
