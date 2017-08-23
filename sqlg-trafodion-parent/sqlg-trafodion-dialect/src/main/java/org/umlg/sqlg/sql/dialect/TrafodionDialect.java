package org.umlg.sqlg.sql.dialect;

import org.apache.commons.lang3.tuple.Triple;
import org.umlg.sqlg.structure.PropertyType;
import org.umlg.sqlg.structure.SchemaTable;
import org.umlg.sqlg.structure.SqlgGraph;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Created by Bin Lin on 8/21/17.
 */

public class TrafodionDialect extends BaseSqlDialect implements SqlBulkDialect {

    @Override
    public String dialectName() {
        return null;
    }

    @Override
    public Set<String> getInternalSchemas() {
        return null;
    }

    @Override
    public PropertyType sqlTypeToPropertyType(SqlgGraph sqlgGraph, String schema, String table, String column, int sqlType, String typeName, ListIterator<Triple<String, Integer, String>> metaDataIter) {
        return null;
    }

    @Override
    public PropertyType sqlArrayTypeNameToPropertyType(String typeName, SqlgGraph sqlgGraph, String schema, String table, String columnName, ListIterator<Triple<String, Integer, String>> metaDataIter) {
        return null;
    }

    @Override
    public void validateProperty(Object key, Object value) {

    }

    @Override
    public String getColumnEscapeKey() {
        return null;
    }

    @Override
    public String getPrimaryKeyType() {
        return null;
    }

    @Override
    public String getAutoIncrementPrimaryKeyConstruct() {
        return null;
    }

    @Override
    public String[] propertyTypeToSqlDefinition(PropertyType propertyType) {
        return new String[0];
    }

    @Override
    public int[] propertyTypeToJavaSqlType(PropertyType propertyType) {
        return new int[0];
    }

    @Override
    public String getForeignKeyTypeDefinition() {
        return null;
    }

    @Override
    public String getArrayDriverType(PropertyType booleanArray) {
        return null;
    }

    @Override
    public String existIndexQuery(SchemaTable schemaTable, String prefix, String indexName) {
        return null;
    }

    @Override
    public Set<String> getSpacialRefTable() {
        return null;
    }

    @Override
    public List<String> getGisSchemas() {
        return null;
    }

    @Override
    public void lockTable(SqlgGraph sqlgGraph, SchemaTable schemaTable, String prefix) {

    }

    @Override
    public void alterSequenceCacheSize(SqlgGraph sqlgGraph, SchemaTable schemaTable, String sequence, int batchSize) {

    }

    @Override
    public long nextSequenceVal(SqlgGraph sqlgGraph, SchemaTable schemaTable, String prefix) {
        return 0;
    }

    @Override
    public long currSequenceVal(SqlgGraph sqlgGraph, SchemaTable schemaTable, String prefix) {
        return 0;
    }

    @Override
    public String sequenceName(SqlgGraph sqlgGraph, SchemaTable outSchemaTable, String prefix) {
        return null;
    }

    @Override
    public boolean supportsBulkWithinOut() {
        return false;
    }

    @Override
    public String afterCreateTemporaryTableStatement() {
        return null;
    }

    @Override
    public List<String> sqlgTopologyCreationScripts() {
        return null;
    }

    @Override
    public String sqlgAddPropertyIndexTypeColumn() {
        return null;
    }

    @Override
    public Object convertArray(PropertyType propertyType, Array array) throws SQLException {
        return null;
    }

    @Override
    public void setArray(PreparedStatement statement, int index, PropertyType type, Object[] values) throws SQLException {

    }

    @Override
    public boolean isSystemIndex(String indexName) {
        return false;
    }

    @Override
    public String valueToValuesString(PropertyType propertyType, Object value) {
        return null;
    }

    @Override
    public boolean supportsType(PropertyType propertyType) {
        return false;
    }
}
