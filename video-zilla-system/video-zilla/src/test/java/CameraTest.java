import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;
import java.util.Optional;
import java.time.Duration;

import static org.junit.Assert.assertEquals;


public class CameraTest {

    @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

    @Test
    public void testReplyWithEmptyReadingIfNoImage() {
        TestProbe<Camera.RespondImages> probe =
            testKit.createTestProbe(Camera.RespondImages.class);
        ActorRef<Camera.Command> deviceActor = testKit.spawn(Camera.create("1", "1"));
        deviceActor.tell(new Camera.ReadImages(42L, probe.getRef()));
        Camera.RespondImages response = probe.receiveMessage(Duration.ofSeconds(20));
        assertEquals(42L, response.requestId);
        assertEquals(500, response.features.size());
    }
}
