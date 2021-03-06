package org.nlogo.extensions.vid

import java.nio.file.{ Files, Paths }

import org.nlogo.api.ExtensionException
import org.scalatest.{ FeatureSpec, GivenWhenThen }

class RecorderSpec extends FeatureSpec with GivenWhenThen with VidHelpers {
  import scala.language.reflectiveCalls

  trait Helpers extends VidSpecHelpers {
    def givenRecorderNotStarted(): Unit = {
      Given("the recorder has not been started")
    }

    def givenRecorderStarted(): Unit = {
      Given("the recorder has been started")
      vid.`start-recorder`()
    }

    def givenRecorderStartedAndReset(): Unit = {
      Given("the recorder has been started and reset")
      vid.`start-recorder`()
      vid.`reset-recorder`()
    }
  }

  feature("recorder-status") {
    scenario("recorder status is inactive before starting") {
      new Helpers {
        givenRecorderNotStarted()
        Then("vid:recorder-status should show \"inactive\"")
        assert(vid.`recorder-status`() == "inactive")
      }
    }

    scenario("recorder status is recording after starting") {
      new Helpers {
        givenRecorderStarted()
        Then("vid:recorder-status should show \"recording\"")
        assert(vid.`recorder-status`() == "recording")
      }
    }

    scenario("recorder status is inactive after reset") {
      new Helpers {
        givenRecorderStartedAndReset()
        Then("vid:recorder-status should show \"inactive\"")
        assert(vid.`recorder-status`() == "inactive")
      }
    }
  }

  feature("start-recorder") {
    scenario("start-recorder errors if a recording has already been started") {
      new Helpers with ExpectError {
        givenRecorderStarted()
        whenRunForError("vid:start-recorder", vid.`start-recorder`())
        thenShouldSeeError("vid: recorder already started")
      }
    }

    scenario("start-recorder errors if supplied with negative width/height") {
      new Helpers with ExpectError {
        whenRunForError("vid:start-recorder -1 -1", vid.`start-recorder`(Double.box(-1), Double.box(-1)))
        thenShouldSeeError("vid: invalid dimensions")
      }
    }

    scenario("start-recorder sets dimensions of recording") {
      new Helpers {
        Given("I have started the recorder with dimensions 640 x 480")
        vid.`start-recorder`(Double.box(640), Double.box(480))
        Then("The recording resolution should be 640 x 480")
        assertResult((640, 480))(recorder.recordingResolution)
      }
    }
  }

  feature("save-recording") {
    scenario("save-recording errors if no recording has been started") {
      new Helpers with ExpectError {
        givenRecorderNotStarted()
        whenRunForError("vid:save-recording", vid.`save-recording`("test.mp4"))
        thenShouldSeeError("vid: recorder not started")
      }
    }

    scenario("save-recording errors if the specified directory does not exist") {
      new Helpers with ExpectError {
        givenRecorderStarted
        whenRunForError("vid:save-recording", vid.`save-recording`("/NoDir/test.mp4"))
        thenShouldSeeError("vid: no such directory")
      }
    }

    scenario("save-recording saves the recording to the specified file") {
      new Helpers {
        givenRecorderStarted
        Given("vid:record-view has been run")
        vid.`record-view`()
        When("vid:save-recording is run")
        vid.`save-recording`("/tmp/recorder-test.mp4")
        Then("I should see that the recording has been saved to the specified file")
        assert(Files.exists(Paths.get("/tmp/recorder-test.mp4")))
      }
    }
    scenario("save-recording saves the file relative to the current path") {
      new Helpers {
        try {
          Files.delete(Paths.get("foo.mp4"))
        } catch { case _: java.nio.file.NoSuchFileException => }
        givenRecorderStarted
        Given("vid:record-view has been run")
        vid.`record-view`()
        When("vid:save-recording is run")
        vid.`save-recording`("foo")
        Then("I should see that the recording has been saved to the specified file")
        assert(Files.exists(Paths.get("foo.mp4")))
      }
    }

    scenario("save-recording throws an exception if no frames have been recorded") {
      new Helpers with ExpectError {
        givenRecorderStarted
        whenRunForError("vid:save-recording", vid.`save-recording`("error.mp4"))
        thenShouldSeeError("vid: no frames recorded")
      }
    }
  }

  feature("record-view") {
    scenario("record-view errors if no recording has been started") {
      new Helpers with ExpectError {
        givenRecorderNotStarted()
        whenRunForError("vid:record-view", vid.`record-view`())
        thenShouldSeeError("vid: recorder not started")
      }
    }

    scenario("record-view records an image once a recording has been started") {
      new Helpers {
        givenRecorderStarted()
        When("vid:record-view is called")
        vid.`record-view`()
        Then("the exported view should be saved to the recording")
        assert(recorder.lastFrame.getHeight == 480)
        assert(recorder.lastFrame.getWidth == 480)
      }
    }
  }

  feature("record-interface") {
    scenario("record-interface records the NetLogo interface") {
      new Helpers {
        givenRecorderStarted()
        When("vid:record-interface is called")
        vid.`record-interface`()
        Then("the exported interface should be saved to the recording")
        assert(recorder.lastFrame.getHeight == 640)
        assert(recorder.lastFrame.getWidth == 640)
      }
    }
  }

  feature("record-source") {
    scenario("errors when no source started") {
      new Helpers with ExpectError {
        givenRecorderStarted()
        whenRunForError("vid:record-source", vid.`record-source`())
        thenShouldSeeError("vid: no selected source")
      }
    }

    scenario("records a frame when a source is active") {
      new Helpers {
        givenOpenMovie()
        givenRecorderStarted()
        When("vid:record-source is called")
        vid.`record-source`()
        Then("the movie image should be recorded")
        assert(recorder.lastFrame == dummyImage)
      }
    }
  }
}
