package com.insightfullogic.honest_profiler.collector;

import com.insightfullogic.honest_profiler.ProfileFixtures;
import com.insightfullogic.honest_profiler.log.Method;
import com.insightfullogic.honest_profiler.log.StackFrame;
import com.insightfullogic.honest_profiler.log.TraceStart;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProfileTreeTest {

    private final FakeProfileListener listener = new FakeProfileListener();
    private final LogCollector collector = new LogCollector(listener);

    @Test
    public void rendersSingleNode() {
        collector.handle(new TraceStart(1, 1));
        collector.handle(new StackFrame(20, ProfileFixtures.printlnId));
        collector.handle(ProfileFixtures.println);
        collector.endOfLog();

        ProfileNode node = assertProfileHasSingleTree();
        assertEquals(0L, node.children().count());
        assertNode(ProfileFixtures.println, 1.0, node);
    }

    @Test
    public void collectsTwoNodesIntoATree() {
        printlnCallingAppend(1);
        collector.endOfLog();

        ProfileNode node = assertProfileHasSingleTree();
        assertEquals(1L, node.children().count());
        assertNode(ProfileFixtures.println, 1.0, node);
        assertNode(ProfileFixtures.append, 1.0, node.children().findFirst().get());
    }

    @Test
    public void collectsSplitMethods() {
        printlnCallingPrintfAndAppend(1);
        collector.endOfLog();

        ProfileNode node = assertProfileHasSingleTree();
        assertPrintlnCallingAppendAndPrintf(node);
    }

    @Test
    public void collectsFromMultipleThreads() {
        printlnCallingPrintfAndAppend(1);
        printlnCallingPrintfAndAppend(2);
        collector.endOfLog();

        List<ProfileTree> trees = listener.getProfile().getTrees();
        assertEquals(2, trees.size());
        trees.forEach(tree -> assertPrintlnCallingAppendAndPrintf(tree.getRootNode()));
    }

    private void assertPrintlnCallingAppendAndPrintf(ProfileNode node) {
        assertEquals(2L, node.children().count());
        assertNode(ProfileFixtures.println, 1.0, node);

        List<ProfileNode> children = node.getChildren();
        assertNode(ProfileFixtures.append, 0.5, children.get(0));
        assertNode(ProfileFixtures.printf, 0.5, children.get(1));
    }

    private void printlnCallingPrintfAndAppend(int threadId) {
        printlnCallingAppend(threadId);
        collector.handle(new TraceStart(2, threadId));
        collector.handle(new StackFrame(20, ProfileFixtures.printfId));
        collector.handle(ProfileFixtures.printf);
        collector.handle(new StackFrame(20, ProfileFixtures.printlnId));
    }

    private void printlnCallingAppend(int threadId) {
        collector.handle(new TraceStart(2, threadId));
        collector.handle(new StackFrame(20, ProfileFixtures.appendId));
        collector.handle(ProfileFixtures.append);
        collector.handle(new StackFrame(20, ProfileFixtures.printlnId));
        collector.handle(ProfileFixtures.println);
    }

    private ProfileNode assertProfileHasSingleTree() {
        Profile profile = listener.getProfile();
        List<ProfileTree> trees = profile.getTrees();
        assertEquals(1, trees.size());

        return trees.get(0).getRootNode();
    }

    private void assertNode(Method method, double ratio, ProfileNode node) {
        assertEquals(ratio, node.getTimeShare(), 0.00001);
        assertEquals(method, node.getMethod());
    }

}
