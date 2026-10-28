package trace.fixture;

import java.util.ArrayList;
import java.util.List;

/** Fixture app run in a subprocess by AgentIntegrationTest. Prints only DONE. */
public class FixtureMain {
    public static class EvilToString {
        @Override public String toString() { throw new IllegalStateException("toString must never be called"); }
    }

    public static class LazyList extends ArrayList<String> {
        @Override public String toString() { throw new IllegalStateException("lazy proxy must never be rendered"); }
    }

    public int add(int a, int b) { return a + b; }

    public int fail(String secret) {
        throw new IllegalStateException("boom for " + secret);
    }

    public String greet(String name) { return "hello " + name; }

    public int loop(int n) { int total = 0; for (int i = 0; i < n; i++) total = add(total, 1); return total; }

    public Object evil() { return new EvilToString(); }

    public List<String> lazy() { return new LazyList(); }

    public static void main(String[] args) {
        FixtureMain fixture = new FixtureMain();
        fixture.greet("world");
        fixture.greet("a@b.com");
        fixture.evil();
        fixture.lazy();
        try { fixture.fail("hunter2-password"); } catch (IllegalStateException expected) { /* app handles it */ }
        if (args.length > 0) fixture.loop(Integer.parseInt(args[0]));
        System.out.println("DONE");
    }
}
