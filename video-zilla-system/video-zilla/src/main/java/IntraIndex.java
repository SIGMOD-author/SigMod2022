import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import com.telmomenezes.jfastemd.*;
import com.hierarchy.index.*;

public class IntraIndex extends AbstractBehavior<IntraIndex.Command> {
    public interface Command {}

    static enum Passivate implements Command {
        INSTANCE
    }

    // data ingestion msg
    public static class WrappedRespondImages implements Command {
        final Camera.RespondImages response;

        public WrappedRespondImages(Camera.RespondImages response) {
            this.response = response;
        }
    }

    public static class ReadFeatures implements Command {
        final long requestId;

        public ReadFeatures(long requestId) {
            this.requestId = requestId;
        }
    }


    // Query msg
    public static final class QuerySignaturesWithSignature implements Command {
        final long requestId;
        final ActorRef<RespondSignatures> replyTo;
        final String label;
        
        public QuerySignaturesWithSignature(
                long requestId, 
                ActorRef<RespondSignatures> replyTo,
                String label) {
            this.requestId = requestId;
            this.replyTo = replyTo;
            this.label = label;
        }
    }

    public static final class QuerySignaturesWithFeature implements Command {
        final long requestId;
        final ActorRef<RespondSignatures> replyTo;
        final FeatureND targetFeature;
        final Set<String> candidateSignatureLabels;

        public QuerySignaturesWithFeature (
                long requestId,
                ActorRef<RespondSignatures> replyTo,
                FeatureND targetFeature,
                Set<String> candididateSignatureLabels) {
            this.requestId = requestId;
            this.replyTo = replyTo;
            this.targetFeature = targetFeature;
            this.candidateSignatureLabels = candididateSignatureLabels;
        }
    }

    // Two types of queries share the same response msg
    public static final class RespondSignatures {
        final long requestId;
        final String deviceId;
        final List<Signature> signatures;

        public RespondSignatures(long requestId, String deviceId, List<Signature> signatures) {
            this.requestId = requestId;
            this.deviceId = deviceId;
            this.signatures = signatures;
        }
    }

    public static Behavior<Command> create(String groupId, String deviceId, 
            ActorRef<InterIndex.Command> interIndex) {
        return Behaviors.setup(context 
                -> new IntraIndex(context, groupId, deviceId, interIndex));
    }

    private final String appId;
    private final String deviceId;
    private ActorRef<Camera.Command> camera;
    private ActorRef<InterIndex.Command> interIndex;
    private IntraCameraIndex intra;

    private IntraIndex(ActorContext<Command> context, String appId, String deviceId, 
            ActorRef<InterIndex.Command> interIndex) {
        super(context);
        this.appId = appId;
        this.deviceId = deviceId;
        this.camera = 
            getContext()
                .spawn(Camera.create(appId, deviceId), "camera-" + deviceId);
        this.interIndex = interIndex;
        // max SVS length is set to be 15 min = 90000 millisecond
        intra = new IntraCameraIndex(appId, deviceId, 90000);

        context.getLog().info("IntraIndex actor {}-{} started", appId, deviceId);
    }

    private IntraIndex onReadFeatures(ReadFeatures r) {
        ActorRef<Camera.RespondImages> respondImagesAdapter = 
            getContext().messageAdapter(Camera.RespondImages.class, WrappedRespondImages::new);
        this.camera.tell(new Camera.ReadImages(1L, respondImagesAdapter));
        return this;
    }

    private IntraIndex onRespondImages(WrappedRespondImages r) {
        getContext().getLog().info("Update the intraIndex of camera {}", deviceId);
        for (FeatureND feature : r.response.features) {
            List<List<Signature>> res = intra.insertFeature(feature);
            if (res.size() == 2) {
                getContext().getLog().info("detected drift");
                interIndex.tell(new InterIndex.RespondFeatures(this.deviceId, res));
            }
        }
        return this;
    }

    private IntraIndex onQuerySignaturesWithSignature(QuerySignaturesWithSignature request) {
        List<Signature> res = intra.clusterQuery(request.label);
        request.replyTo.tell(new RespondSignatures(request.requestId, deviceId, res));
        return this;
    }

    private IntraIndex onQuerySignaturesWithFeature(QuerySignaturesWithFeature request) {
        List<Signature> res = intra.directQuery(
                request.targetFeature, 
                request.candidateSignatureLabels);
        request.replyTo.tell(new RespondSignatures(request.requestId, deviceId, res));
        return this;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(Passivate.class, m -> Behaviors.stopped())
            .onMessage(ReadFeatures.class, this::onReadFeatures)
            .onMessage(WrappedRespondImages.class, this::onRespondImages)
            .onMessage(QuerySignaturesWithSignature.class, this::onQuerySignaturesWithSignature)
            .onMessage(QuerySignaturesWithFeature.class, this::onQuerySignaturesWithFeature)
            .onSignal(PostStop.class, signal -> onPostStop())
            .build();
    }

    private Behavior<Command> onPostStop() {
        getContext().getLog().info("IntraIndex actor {}-{} stopped", appId, deviceId);
        return Behaviors.stopped();
    }
}
