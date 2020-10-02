import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.typed.javadsl.TimerScheduler;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.telmomenezes.jfastemd.*;

public class ClusterQuery extends AbstractBehavior<ClusterQuery.Command> {
    public interface Command {}

    private static enum CollectionTimeout implements Command {
        INSTANCE
    }

    static class WrappedRespondSignatures implements Command {
        final IntraIndex.RespondSignatures response;

        WrappedRespondSignatures(IntraIndex.RespondSignatures response) {
            this.response = response;
        }
    }

    private static class CameraTerminated implements Command {
        final String deviceId;

        private CameraTerminated(String deviceId) {
            this.deviceId = deviceId;
        }
    }

    public static Behavior<Command> create(
            Map<String, ActorRef<IntraIndex.Command>> deviceIdToActor,
            long requestId,
            String signatureLabel,
            ActorRef<IndexSupervisor.Command> requester,
            Duration timeout) {
        return Behaviors.setup(
            context -> 
                Behaviors.withTimers(
                    timers -> 
                        new ClusterQuery(
                            deviceIdToActor, requestId, signatureLabel, requester, timeout, context, timers)));
    }

    private final long requestId;
    private final ActorRef<IndexSupervisor.Command> requester;
    private final Map<String, IndexSupervisor.SignatureResult> repliesSoFar = new HashMap<>();
    private final Set<String> stillWaiting;

    public ClusterQuery(
            Map<String, ActorRef<IntraIndex.Command>> deviceIdToActor,
            long requestId,
            String signatureLabel,
            ActorRef<IndexSupervisor.Command> requester,
            Duration timeout,
            ActorContext<Command> context,
            TimerScheduler<Command> timers) {
        super(context);
        this.requestId = requestId;
        this.requester = requester;

        timers.startSingleTimer(CollectionTimeout.INSTANCE, timeout);

        ActorRef<IntraIndex.RespondSignatures> respondSignatureAdapter =
            context.messageAdapter(IntraIndex.RespondSignatures.class, WrappedRespondSignatures::new);

        for (Map.Entry<String, ActorRef<IntraIndex.Command>> entry : deviceIdToActor.entrySet()) {
            context.watchWith(entry.getValue(), new CameraTerminated(entry.getKey()));
            entry
                .getValue()
                .tell(new IntraIndex.QuerySignaturesWithSignature(
                            0L, respondSignatureAdapter, signatureLabel));
        }
        stillWaiting = new HashSet<>(deviceIdToActor.keySet());
    } 

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(WrappedRespondSignatures.class, this::onRespondSignature)
            .onMessage(CameraTerminated.class, this::onCameraTerminated)
            .onMessage(CollectionTimeout.class, this::onCollectionTimeout)
            .build();
    }

    private Behavior<Command> onRespondSignature(WrappedRespondSignatures r) {
        IndexSupervisor.SignatureResult res = new IndexSupervisor.SignatureList(r.response.signatures);
            
        String deviceId = r.response.deviceId;
        repliesSoFar.put(deviceId, res);
        stillWaiting.remove(deviceId);

        return respondWhenAllCollected();
    }

    private Behavior<Command> onCameraTerminated(CameraTerminated terminated) {
        if (stillWaiting.contains(terminated.deviceId)) {
            repliesSoFar.put(terminated.deviceId, IndexSupervisor.CameraNotAvailable.INSTANCE);
            stillWaiting.remove(terminated.deviceId);
        }
        return respondWhenAllCollected();
    }

    private Behavior<Command> onCollectionTimeout(CollectionTimeout timeout) {
        for (String deviceId : stillWaiting) {
            repliesSoFar.put(deviceId, IndexSupervisor.CameraTimeOut.INSTANCE);
        }
        stillWaiting.clear();
        return respondWhenAllCollected();
    }

    private Behavior<Command> respondWhenAllCollected() {
        if (stillWaiting.isEmpty()) {
            requester.tell(new IndexSupervisor.RespondAllSignatures(requestId, repliesSoFar));
            return Behaviors.stopped();
        } else {
            return this;
        }
    }
}
