package org.umlg.sqlg.test.gremlincompile;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.process.traversal.step.TraversalOptionParent;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.MapHelper;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Assert;
import org.junit.Test;
import org.umlg.sqlg.test.BaseTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: 2017/02/06
 * Time: 9:27 AM
 */
public class TestGremlinCompileChoose extends BaseTest {

    @Test
    public void g_V_hasLabelXpersonX_asXp1X_chooseXoutEXknowsX__outXknowsXX_asXp2X_selectXp1_p2X_byXnameX() {
        loadModern();
        final Traversal<Vertex, Map<String, String>> traversal = this.sqlgGraph.traversal()
                .V()
                .hasLabel("person").as("p1")
                .choose(
                        __.outE("knows"), __.out("knows")
                ).as("p2")
                .<String>select("p1", "p2").by("name");
        printTraversalForm(traversal);
        checkResults(makeMapList(2,
                "p1", "marko", "p2", "vadas",
                "p1", "marko", "p2", "josh",
                "p1", "vadas", "p2", "vadas",
                "p1", "josh", "p2", "josh",
                "p1", "peter", "p2", "peter"
        ), traversal);
    }

    @Test
    public void g_V_chooseXhasLabelXpersonX_and_outXcreatedX__outXknowsX__identityX_name() {
        loadModern();

        GraphTraversal<Vertex, String> traversal = this.sqlgGraph.traversal()
                .V()
                .choose(
                        __.hasLabel("person").and().out("created"),
                        __.out("knows"),
                        __.identity()
                ).values("name");
        checkResults(Arrays.asList("lop", "ripple", "josh", "vadas", "vadas"), traversal);
    }

    //not optimized
    @Test
    public void g_V_chooseXlabelX_optionXblah__outXknowsXX_optionXbleep__outXcreatedXX_optionXnone__identityX_name() {
        loadModern();
        DefaultGraphTraversal<Vertex, String> traversal = (DefaultGraphTraversal)this.sqlgGraph.traversal().V().choose(__.label())
                .option("blah", __.out("knows"))
                .option("bleep", __.out("created"))
                .option(TraversalOptionParent.Pick.none, __.identity()).values("name");

        Assert.assertEquals(3, traversal.getSteps().size());
        printTraversalForm(traversal);
        Assert.assertEquals(3, traversal.getSteps().size());
        checkResults(Arrays.asList("marko", "vadas", "peter", "josh", "lop", "ripple"), traversal);
    }

    @Test
    public void g_V_chooseXout_countX_optionX2L__nameX_optionX3L__valueMapX() {
        loadModern();
        final Traversal<Vertex, Object> traversal =  this.sqlgGraph.traversal()
                .V()
                .choose(__.out().count())
                .option(2L, __.values("name"))
                .option(3L, __.valueMap());
        printTraversalForm(traversal);
        final Map<String, Long> counts = new HashMap<>();
        int counter = 0;
        while (traversal.hasNext()) {
            MapHelper.incr(counts, traversal.next().toString(), 1l);
            counter++;
        }
        Assert.assertFalse(traversal.hasNext());
        Assert.assertEquals(2, counter);
        Assert.assertEquals(2, counts.size());
        Assert.assertEquals(Long.valueOf(1), counts.get("{name=[marko], age=[29]}"));
        Assert.assertEquals(Long.valueOf(1), counts.get("josh"));
    }
}
