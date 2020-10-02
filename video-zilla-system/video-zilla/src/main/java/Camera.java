import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Optional;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Queue;
import java.io.File;
import java.io.FileNotFoundException;
import com.telmomenezes.jfastemd.*;

public class Camera extends AbstractBehavior<Camera.Command> {
    public interface Command {}

    public static final class ReadImages implements Command {
        final long requestId;
        final ActorRef<RespondImages> replyTo;

        public ReadImages(long requestId, ActorRef<RespondImages> replyTo) {
            this.requestId = requestId;
            this.replyTo = replyTo;
        }
    }

    public static final class RespondImages {
        final long requestId;
        final Queue<FeatureND> features;

        public RespondImages(long requestId, Queue<FeatureND> features) {
            this.requestId = requestId;
            this.features = new LinkedList<>(features);
        }
    }

    public static Behavior<Command> create(String appId, String deviceId) {
        return Behaviors.setup(context -> new Camera(context, appId, deviceId));
    }

    private final String appId;
    private final String deviceId;
    private Queue<FeatureND> features;

    private Camera(ActorContext<Command> context, String appId, String deviceId) {
        super(context);
        this.appId = appId;
        this.deviceId = deviceId;
        String filename = "./data/feature" + appId + "-" + deviceId + ".csv";
        try {
            features = getFeatures(filename, 1024);
        } catch (FileNotFoundException e) {
            context.getLog().info
                ("Camera actor {}-{} terminated because it can not open the file", appId, deviceId);
        } 
        context.getLog().info("Camera actor {}-{} started", appId, deviceId);
    }

    private Queue<FeatureND> getFeatures(String fileName, int dimension) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName)); 
        List<Double> list = new ArrayList<>();
        while (scanner.hasNextDouble()) {
            list.add(scanner.nextDouble());
        }
        scanner.close();

        int n = list.size() / dimension;
        Queue<FeatureND> features = new LinkedList<FeatureND>();

        for (int i = 0; i < n; i++) {
            List<Double> tmp = new LinkedList<>();
            for (int j = 0; j < dimension; j++) {
                tmp.add(list.get(i + j * n));
            }
            FeatureND f = new FeatureND(tmp);
            features.add(f);
        }
        return features;
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
            .onMessage(ReadImages.class, this::onReadImages)
            .onSignal(PostStop.class, signal -> onPostStop())
            .build();
    }

    private Behavior<Command> onReadImages(ReadImages r) {
        getContext().getLog().info("Receive the read image message");
        r.replyTo.tell(new RespondImages(r.requestId, features));
        return this;
    }

    private Camera onPostStop() {
        getContext().getLog().info("Camera actor {}-{} stopped", appId, deviceId);
        return this;
    }
}
