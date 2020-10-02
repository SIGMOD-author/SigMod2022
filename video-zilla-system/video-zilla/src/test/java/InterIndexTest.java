import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class InterIndexTest {

  @ClassRule public static final TestKitJunitResource testKit = new TestKitJunitResource();

  //@Test
  //public void testReplyToRegistrationRequests() {
  //  TestProbe<IndexSupervisor.CameraRegistered> probe = testKit.createTestProbe(IndexSupervisor.CameraRegistered.class);
  //  ActorRef<InterIndex.Command> interIndexActor = testKit.spawn(InterIndex.create("1"));

  //  interIndexActor.tell(new IndexSupervisor.RequestTrackCamera("1", "1", probe.getRef()));
  //  IndexSupervisor.CameraRegistered registered1 = probe.receiveMessage();
  //  try {
  //      TimeUnit.SECONDS.sleep(3);
  //  } catch (Exception e) {
  //  }
  //  registered1.intraIndex.tell(new IntraIndex.ReadFeatures(1));
  //  try {
  //      TimeUnit.SECONDS.sleep(3);
  //  } catch (Exception e) {
  //  }

  //  // another deviceId
  //  //interIndexActor.tell(new IndexSupervisor.RequestTrackCamera("1", "3", probe.getRef()));
  //  //IndexSupervisor.CameraRegistered registered2 = probe.receiveMessage();
  //  //registered2.intraIndex.tell(new IntraIndex.ReadFeatures(1, null));
  //  //assertNotEquals(registered1.intraIndex, registered2.intraIndex);
  //}

  @Test
  public void testReplyToRegistrationRequests() {
    TestProbe<IndexSupervisor.CameraRegistered> probe = testKit.createTestProbe(IndexSupervisor.CameraRegistered.class);
    ActorRef<IndexSupervisor.Command> sup = testKit.spawn(IndexSupervisor.create());

    sup.tell(new IndexSupervisor.UserTrackCamera("1", "1"));
    try {
        TimeUnit.SECONDS.sleep(3);
    } catch (Exception e) {
    }

    // another deviceId
    //interIndexActor.tell(new IndexSupervisor.RequestTrackCamera("1", "3", probe.getRef()));
    //IndexSupervisor.CameraRegistered registered2 = probe.receiveMessage();
    //registered2.intraIndex.tell(new IntraIndex.ReadFeatures(1, null));
    //assertNotEquals(registered1.intraIndex, registered2.intraIndex);
  }
}
//  @Test
//  public void testIgnoreWrongRegistrationRequests() {
//      TestProbe<IndexSupervisor.CameraRegistered> probe = testKit.createTestProbe(IndexSupervisor.CameraRegistered.class);
//      ActorRef<InterIndex.Command> interIndexActor = testKit.spawn(InterIndex.create("app"));
//      interIndexActor.tell(new IndexSupervisor.RequestTrackCamera("wrong app", "device1", probe.getRef()));
//      probe.expectNoMessage();
//  }
//
//  @Test
//  public void testListActiveDevices() {
//      TestProbe<IndexSupervisor.CameraRegistered> registeredProbe = testKit.createTestProbe(IndexSupervisor.CameraRegistered.class);
//      ActorRef<InterIndex.Command> interIndexActor = testKit.spawn(InterIndex.create("app"));
//
//      interIndexActor.tell(new IndexSupervisor.RequestTrackCamera("app", "device1", registeredProbe.getRef()));
//      registeredProbe.receiveMessage();
//
//      interIndexActor.tell(new IndexSupervisor.RequestTrackCamera("app", "device2", registeredProbe.getRef()));
//      registeredProbe.receiveMessage();
//
//      TestProbe<IndexSupervisor.ReplyCameraList> deviceListProbe = testKit.createTestProbe(IndexSupervisor.ReplyCameraList.class);
//
//      interIndexActor.tell(new IndexSupervisor.RequestCameraList(0L, "app", deviceListProbe.getRef()));
//      IndexSupervisor.ReplyCameraList reply = deviceListProbe.receiveMessage();
//      assertEquals(0L, reply.requestId);
//      assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);
//  }
//
//  @Test
//  public void testListActiveDevicesAfterOneShutsDown() {
//      TestProbe<IndexSupervisor.CameraRegistered> registeredProbe = testKit.createTestProbe(IndexSupervisor.CameraRegistered.class);
//      ActorRef<InterIndex.Command> interIndexActor = testKit.spawn(InterIndex.create("app"));
//
//      interIndexActor.tell(new IndexSupervisor.RequestTrackCamera("app", "device1", registeredProbe.getRef()));
//      IndexSupervisor.CameraRegistered registered1 = registeredProbe.receiveMessage();
//
//      interIndexActor.tell(new IndexSupervisor.RequestTrackCamera("app", "device2", registeredProbe.getRef()));
//      IndexSupervisor.CameraRegistered registered2 = registeredProbe.receiveMessage();
//
//      ActorRef<IntraIndex.Command> toShutDown = registered1.intraIndex;
//
//      TestProbe<IndexSupervisor.ReplyCameraList> deviceListProbe = testKit.createTestProbe(IndexSupervisor.ReplyCameraList.class);
//
//      interIndexActor.tell(new IndexSupervisor.RequestCameraList(0L, "app", deviceListProbe.getRef()));
//      IndexSupervisor.ReplyCameraList reply = deviceListProbe.receiveMessage();
//      assertEquals(0L, reply.requestId);
//      assertEquals(Stream.of("device1", "device2").collect(Collectors.toSet()), reply.ids);
//
//      toShutDown.tell(IntraIndex.Passivate.INSTANCE);
//      registeredProbe.expectTerminated(toShutDown, registeredProbe.getRemainingOrDefault());
//
//      // using awaitAssert to retry because it might take longer for the interIndexActor
//      // to see the Terminated, that order is undefined
//      registeredProbe.awaitAssert(
//              () -> {
//                  interIndexActor.tell(new IndexSupervisor.RequestCameraList(1L, "app", deviceListProbe.getRef()));
//                  IndexSupervisor.ReplyCameraList r = deviceListProbe.receiveMessage();
//                  assertEquals(1L, r.requestId);
//                  assertEquals(Stream.of("device2").collect(Collectors.toSet()), r.ids);
//                  return null;
//              });
//  }
//}
