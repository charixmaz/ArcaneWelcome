package dev.arcane.welcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class WelcomeTest {
    @TempDir Path dir;
    @Test void exactChatMatching() {
        assertEquals("New_Player",WelcomeText.target("Welcome New_Player","welcome").orElseThrow());
        assertEquals("Steve",WelcomeText.target("welcome @Steve","welcome").orElseThrow());
        assertTrue(WelcomeText.target("/welcome Steve","welcome").isEmpty());
        assertTrue(WelcomeText.target("I welcome Steve","welcome").isEmpty());
        assertTrue(WelcomeText.target("welcome Steve extra","welcome").isEmpty());
        assertTrue(WelcomeText.target("welcome Steve;op","welcome").isEmpty());
    }
    @Test void restartDoesNotResetEligibilityOrClaims() throws Exception {
        Path file=dir.resolve("welcome.db");
        try(Store db=new Store(file)) {
            assertEquals(100,db.join("new","Steve",true,100,400).starts());
            assertTrue(db.reserve("helper","new"));
            assertFalse(db.reserve("helper","new"));
            db.status("helper","new","dispatched");
        }
        try(Store db=new Store(file)) {
            var a=db.join("new","Renamed",true,999,2000);
            assertEquals(100,a.starts());assertEquals(400,a.ends());
            assertFalse(db.reserve("helper","new"));
            assertTrue(db.reserve("other","new"));
        }
    }
    @Test void existingPlayersAreNotNewAndUncertainClaimsStayBlocked() throws Exception {
        try(Store db=new Store(dir.resolve("welcome.db"))) {
            assertEquals(0,db.join("old","Old",false,100,400).ends());
            assertTrue(db.reserve("helper","new"));
        }
        try(Store db=new Store(dir.resolve("welcome.db"))) {
            assertFalse(db.reserve("helper","new"));
        }
    }
    @Test void concurrentConnectionsCannotDoubleClaim() throws Exception {
        Path file=dir.resolve("welcome.db");
        try(Store first=new Store(file);Store second=new Store(file)) {
            var executor=Executors.newFixedThreadPool(2);
            try {
                var a=executor.submit(() -> first.reserve("helper","new"));
                var b=executor.submit(() -> second.reserve("helper","new"));
                assertNotEquals(a.get(),b.get());
            } finally {executor.shutdownNow();}
        }
    }
}
