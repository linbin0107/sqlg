package org.umlg.sqlg.structure;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.graph.step.sideEffect.GraphStep;
import com.tinkerpop.gremlin.process.util.TraverserIterator;
import com.tinkerpop.gremlin.structure.*;
import com.tinkerpop.gremlin.structure.util.HasContainer;
import com.tinkerpop.gremlin.util.StreamFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

/**
 * Date: 2014/07/12
 * Time: 5:49 AM
 */
public class SqlgGraphStep<E extends Element> extends GraphStep<E> {

    private Logger logger = LoggerFactory.getLogger(SchemaManager.class.getName());
    private SqlG sqlG;
    public final List<HasContainer> hasContainers = new ArrayList<>();

    public SqlgGraphStep(final Traversal traversal, final Class<E> returnClass, final SqlG sqlG) {
        super(traversal, returnClass);
        this.sqlG = sqlG;
    }

    @Override
    public void generateTraverserIterator(boolean trackPaths) {
        this.sqlG.tx().readWrite();
        this.starts.clear();
        this.starts.add(new TraverserIterator(this, trackPaths, Vertex.class.isAssignableFrom(this.returnClass) ? this.vertices() : this.edges()));
    }

    @Override
    public void clear() {
        this.starts.clear();
    }

    private Iterator<? extends Edge> edges() {
        Stream<? extends Edge> edgeStream;
        if (this.hasContainers.size() > 1 && this.hasContainers.get(0).key.equals(Element.LABEL) && this.hasContainers.get(1).predicate.equals(Compare.EQUAL)) {
            //Scenario 1, using labeled index via 2 HasContainer
            final HasContainer hasContainer1 = this.hasContainers.get(0);
            final HasContainer hasContainer2 = this.hasContainers.get(1);
            this.hasContainers.remove(hasContainer1);
            this.hasContainers.remove(hasContainer2);
            edgeStream = getEdgesUsingLabeledIndex((String) hasContainer1.value, hasContainer2.key, hasContainer2.value);
        } else if (this.hasContainers.size() > 0 && this.hasContainers.get(0).key.equals(Element.LABEL)) {
            HasContainer hasContainer = this.hasContainers.get(0);
            //Scenario 2, using label only for search
            if (hasContainer.predicate == Contains.IN || hasContainer.predicate == Contains.NOT_IN) {
                final List<String> labels = (List<String>) hasContainer.value;
                edgeStream = getEdgesUsingLabel(labels.toArray(new String[labels.size()]));
            } else {
                edgeStream = getEdgesUsingLabel((String) hasContainer.value);
            }
            this.hasContainers.remove(hasContainer);
        } else {
            edgeStream = getEdges();
        }
        return edgeStream.filter(e -> HasContainer.testAll((Edge) e, this.hasContainers)).iterator();

    }

    private Iterator<? extends Vertex> vertices() {
        Stream<? extends Vertex> vertexStream;
        if (this.hasContainers.size() > 1 && this.hasContainers.get(0).key.equals(Element.LABEL) && this.hasContainers.get(1).predicate.equals(Compare.EQUAL)) {
            //Scenario 1, using labeled index via 2 HasContainer
            final HasContainer hasContainer1 = this.hasContainers.get(0);
            final HasContainer hasContainer2 = this.hasContainers.get(1);
            this.hasContainers.remove(hasContainer1);
            this.hasContainers.remove(hasContainer2);
            vertexStream = getVerticesUsingLabeledIndex((String) hasContainer1.value, hasContainer2.key, hasContainer2.value);
        } else if (this.hasContainers.size() > 0 && this.hasContainers.get(0).key.equals(Element.LABEL)) {
            //Scenario 2, using label only for search
            HasContainer hasContainer = this.hasContainers.get(0);
            if (hasContainer.predicate == Contains.IN || hasContainer.predicate == Contains.NOT_IN) {
                final List<String> labels = (List<String>) hasContainer.value;
                vertexStream = getVerticesUsingLabel(labels.toArray(new String[labels.size()]));
            } else {
                vertexStream = getVerticesUsingLabel((String) hasContainer.value);
            }
            this.hasContainers.remove(hasContainer);
        } else {
            vertexStream = getVertices();
        }
        return vertexStream.filter(v -> HasContainer.testAll((Vertex) v, this.hasContainers)).iterator();
    }

    private Stream<? extends Vertex> getVertices() {
        return StreamFactory.stream(this._vertices());
    }

    private Stream<? extends Edge> getEdges() {
        return StreamFactory.stream(this._edges());
    }

    private Stream<? extends Vertex> getVerticesUsingLabel(String... labels) {
        Stream<? extends Vertex> vertices = Stream.empty();
        for (String label : labels) {
            vertices = Stream.concat(vertices, StreamFactory.stream(this._verticesUsingLabel(label)));
        }
        return vertices;
    }

    private Stream<? extends Vertex> getVerticesUsingLabeledIndex(String label, String key, Object value) {
        return StreamFactory.stream(this._verticesUsingLabeledIndex(label, key, value));
    }

    private Stream<? extends Edge> getEdgesUsingLabel(String... labels) {
        Stream<? extends Edge> edges = Stream.empty();
        for (String label : labels) {
            edges = Stream.concat(edges, StreamFactory.stream(this._edgesUsingLabel(label)));
        }
        return edges;
    }

    private Stream<? extends Edge> getEdgesUsingLabeledIndex(String label, String key, Object value) {
        return StreamFactory.stream(this._edgesUsingLabeledIndex(label, key, value));
    }

    private Iterable<? extends Vertex> _verticesUsingLabel(String label) {
        Set<String> schemas;
        SchemaTable schemaTable = SqlgUtil.parseLabelMaybeNoSchema(label);
        if (schemaTable.getSchema() == null) {
            schemas = this.sqlG.getSchemaManager().getSchemasForTable(SchemaManager.VERTEX_PREFIX + schemaTable.getTable());
        } else {
            schemas = new HashSet<>();
            schemas.add(schemaTable.getSchema());
        }
        List<SqlgVertex> sqlGVertexes = new ArrayList<>();
        //Schemas are null when has('label') searches for a label that does not exist yet.
        if (schemas != null) {
            for (String schema : schemas) {
                StringBuilder sql = new StringBuilder("SELECT * FROM ");
                sql.append(this.sqlG.getSqlDialect().maybeWrapInQoutes(schema));
                sql.append(".");
                sql.append(this.sqlG.getSqlDialect().maybeWrapInQoutes(SchemaManager.VERTEX_PREFIX + schemaTable.getTable()));
                if (this.sqlG.getSqlDialect().needsSemicolon()) {
                    sql.append(";");
                }
                Connection conn = this.sqlG.tx().getConnection();
                if (logger.isDebugEnabled()) {
                    logger.debug(sql.toString());
                }
                try (PreparedStatement preparedStatement = conn.prepareStatement(sql.toString())) {
                    ResultSet resultSet = preparedStatement.executeQuery();
                    while (resultSet.next()) {
                        long id = resultSet.getLong(1);
                        SqlgVertex sqlGVertex = new SqlgVertex(this.sqlG, id, schema, schemaTable.getTable());
                        sqlGVertex.loadResultSet(resultSet);
                        sqlGVertexes.add(sqlGVertex);
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return sqlGVertexes;
    }

    private Iterable<? extends Vertex> _verticesUsingLabeledIndex(String label, String key, Object value) {
        SchemaTable schemaTable = SqlgUtil.parseLabel(label, this.sqlG.getSqlDialect().getPublicSchema());
        //Check that the table and column exist
        List<SqlgVertex> sqlGVertexes = new ArrayList<>();
        if (this.sqlG.getSchemaManager().tableExist(schemaTable.getSchema(), SchemaManager.VERTEX_PREFIX + schemaTable.getTable()) &&
                this.sqlG.getSchemaManager().columnExists(schemaTable.getSchema(), SchemaManager.VERTEX_PREFIX + schemaTable.getTable(), key)) {
            StringBuilder sql = new StringBuilder("SELECT * FROM ");
            sql.append(this.sqlG.getSchemaManager().getSqlDialect().maybeWrapInQoutes(schemaTable.getSchema()));
            sql.append(".");
            sql.append(this.sqlG.getSchemaManager().getSqlDialect().maybeWrapInQoutes(SchemaManager.VERTEX_PREFIX + schemaTable.getTable()));
            sql.append(" a WHERE a.");
            sql.append(this.sqlG.getSchemaManager().getSqlDialect().maybeWrapInQoutes(key));
            sql.append(" = ?");
            if (this.sqlG.getSqlDialect().needsSemicolon()) {
                sql.append(";");
            }
            Connection conn = this.sqlG.tx().getConnection();
            if (logger.isDebugEnabled()) {
                logger.debug(sql.toString());
            }
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql.toString())) {
                Map<String, Object> keyValueMap = new HashMap<>();
                keyValueMap.put(key, value);
                SqlgElement.setKeyValuesAsParameter(this.sqlG, 1, conn, preparedStatement, keyValueMap);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    long id = resultSet.getLong(1);
                    SqlgVertex sqlGVertex = new SqlgVertex(this.sqlG, id, schemaTable.getSchema(), schemaTable.getTable());
                    sqlGVertex.loadResultSet(resultSet);
                    sqlGVertexes.add(sqlGVertex);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return sqlGVertexes;
    }

    private Iterable<? extends Vertex> _vertices() {
        List<SqlgVertex> sqlGVertexes = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(this.sqlG.getSqlDialect().maybeWrapInQoutes(this.sqlG.getSqlDialect().getPublicSchema()));
        sql.append(".");
        sql.append(this.sqlG.getSqlDialect().maybeWrapInQoutes(SchemaManager.VERTICES));
        if (this.sqlG.getSqlDialect().needsSemicolon()) {
            sql.append(";");
        }
        Connection conn = this.sqlG.tx().getConnection();
        if (logger.isDebugEnabled()) {
            logger.debug(sql.toString());
        }
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql.toString())) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                String schema = resultSet.getString(2);
                String table = resultSet.getString(3);
                SqlgVertex sqlGVertex = new SqlgVertex(this.sqlG, id, schema, table);
                sqlGVertexes.add(sqlGVertex);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return sqlGVertexes;
    }

    private Iterable<? extends Edge> _edgesUsingLabel(String label) {
        Set<String> schemas;
        SchemaTable schemaTable = SqlgUtil.parseLabelMaybeNoSchema(label);
        if (schemaTable.getSchema() == null) {
            schemas = this.sqlG.getSchemaManager().getSchemasForTable(SchemaManager.EDGE_PREFIX + schemaTable.getTable());
        } else {
            schemas = new HashSet<>();
            schemas.add(schemaTable.getSchema());
        }
        List<SqlgEdge> sqlGEdges = new ArrayList<>();
        for (String schema : schemas) {
            StringBuilder sql = new StringBuilder("SELECT * FROM ");
            sql.append(this.sqlG.getSchemaManager().getSqlDialect().maybeWrapInQoutes(schema));
            sql.append(".");
            sql.append(this.sqlG.getSchemaManager().getSqlDialect().maybeWrapInQoutes(SchemaManager.EDGE_PREFIX + schemaTable.getTable()));
            if (this.sqlG.getSqlDialect().needsSemicolon()) {
                sql.append(";");
            }
            Connection conn = this.sqlG.tx().getConnection();
            if (logger.isDebugEnabled()) {
                logger.debug(sql.toString());
            }
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql.toString())) {
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    long id = resultSet.getLong(1);
                    SqlgEdge sqlGEdge = new SqlgEdge(this.sqlG, id, schema, schemaTable.getTable());
                    //TODO properties needs to be cached
                    sqlGEdge.loadResultSet(resultSet);
                    sqlGEdges.add(sqlGEdge);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return sqlGEdges;
    }

    private Iterable<? extends Edge> _edgesUsingLabeledIndex(String label, String key, Object value) {
        List<SqlgEdge> sqlGEdges = new ArrayList<>();
        SchemaTable schemaTable = SqlgUtil.parseLabel(label, this.sqlG.getSqlDialect().getPublicSchema());
        if (this.sqlG.getSchemaManager().tableExist(schemaTable.getSchema(), SchemaManager.EDGE_PREFIX + schemaTable.getTable()) &&
                this.sqlG.getSchemaManager().columnExists(schemaTable.getSchema(), SchemaManager.EDGE_PREFIX + schemaTable.getTable(), key)) {
            StringBuilder sql = new StringBuilder("SELECT * FROM ");
            sql.append(this.sqlG.getSchemaManager().getSqlDialect().maybeWrapInQoutes(schemaTable.getSchema()));
            sql.append(".");
            sql.append(this.sqlG.getSchemaManager().getSqlDialect().maybeWrapInQoutes(SchemaManager.EDGE_PREFIX + schemaTable.getTable()));
            sql.append(" a WHERE a.");
            sql.append(this.sqlG.getSchemaManager().getSqlDialect().maybeWrapInQoutes(key));
            sql.append(" = ?");
            if (this.sqlG.getSqlDialect().needsSemicolon()) {
                sql.append(";");
            }
            Connection conn = this.sqlG.tx().getConnection();
            if (logger.isDebugEnabled()) {
                logger.debug(sql.toString());
            }
            try (PreparedStatement preparedStatement = conn.prepareStatement(sql.toString())) {
                Map<String, Object> keyValueMap = new HashMap<>();
                keyValueMap.put(key, value);
                SqlgElement.setKeyValuesAsParameter(this.sqlG, 1, conn, preparedStatement, keyValueMap);
                ResultSet resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    long id = resultSet.getLong(1);
                    SqlgEdge sqlGEdge = new SqlgEdge(this.sqlG, id, schemaTable.getSchema(), schemaTable.getTable());
                    //TODO properties needs to be cached
                    sqlGEdge.loadResultSet(resultSet);
                    sqlGEdges.add(sqlGEdge);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        return sqlGEdges;
    }

    private Iterable<? extends Edge> _edges() {
        List<SqlgEdge> sqlGEdges = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(this.sqlG.getSqlDialect().maybeWrapInQoutes(this.sqlG.getSqlDialect().getPublicSchema()));
        sql.append(".");
        sql.append(this.sqlG.getSqlDialect().maybeWrapInQoutes(SchemaManager.EDGES));
        if (this.sqlG.getSqlDialect().needsSemicolon()) {
            sql.append(";");
        }
        Connection conn = this.sqlG.tx().getConnection();
        if (logger.isDebugEnabled()) {
            logger.debug(sql.toString());
        }
        try (PreparedStatement preparedStatement = conn.prepareStatement(sql.toString())) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                String schema = resultSet.getString(2);
                String table = resultSet.getString(3);
                SchemaTable schemaTablePair = SchemaTable.of(schema, table);
                SqlgEdge sqlGEdge = new SqlgEdge(this.sqlG, id, schemaTablePair.getSchema(), schemaTablePair.getTable());
                sqlGEdges.add(sqlGEdge);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return sqlGEdges;
    }

}
