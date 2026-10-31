package org.big.erd.generator.sql;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.big.erd.entityRelationship.Attribute;

/**
 * Vendor-independent relational schema computed from an ER model.
 * The {@link SqlGenerator} first builds this schema and then renders it as SQL.
 */
public class SqlSchema {

	private final Map<String, Table> tables = new LinkedHashMap<>();

	public Table addTable(String name) {
		Table table = new Table(name);
		tables.put(name, table);
		return table;
	}

	public Table getTable(String name) {
		return tables.get(name);
	}

	public List<Table> getTables() {
		return new ArrayList<>(tables.values());
	}

	public static class Table {
		private final String name;
		private final List<Column> columns = new ArrayList<>();
		private final List<String> primaryKey = new ArrayList<>();
		private final List<List<String>> uniqueKeys = new ArrayList<>();
		private final List<ForeignKey> foreignKeys = new ArrayList<>();
		private final List<String> notes = new ArrayList<>();

		Table(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public List<Column> getColumns() {
			return columns;
		}

		public List<String> getColumnNames() {
			List<String> names = new ArrayList<>();
			for (Column column : columns) {
				names.add(column.getName());
			}
			return names;
		}

		public List<String> getPrimaryKey() {
			return primaryKey;
		}

		public List<List<String>> getUniqueKeys() {
			return uniqueKeys;
		}

		public List<ForeignKey> getForeignKeys() {
			return foreignKeys;
		}

		/** Explanations (e.g. constraints that cannot be enforced), rendered as SQL comments. */
		public List<String> getNotes() {
			return notes;
		}
	}

	public static class Column {
		private final String name;
		private final Attribute attribute;
		private final String renamedFrom;
		private Boolean nullable;

		/**
		 * @param name column name
		 * @param attribute ER attribute that defines the data type
		 * @param renamedFrom original name if the column had to be renamed to stay unique, otherwise null
		 */
		public Column(String name, Attribute attribute, String renamedFrom) {
			this.name = name;
			this.attribute = attribute;
			this.renamedFrom = renamedFrom;
		}

		public String getName() {
			return name;
		}

		public Attribute getAttribute() {
			return attribute;
		}

		public String getRenamedFrom() {
			return renamedFrom;
		}

		/** Explicit nullability, or null to derive it from the attribute (optional attributes are nullable). */
		public Boolean getNullable() {
			return nullable;
		}

		public void setNullable(Boolean nullable) {
			this.nullable = nullable;
		}
	}

	public static class ForeignKey {
		private final String name;
		private final List<String> columns;
		private final String refTable;
		private final List<String> refColumns;
		private final String onDelete;

		/**
		 * @param onDelete referential action such as "CASCADE" or "SET NULL", or null for the database default
		 */
		public ForeignKey(String name, List<String> columns, String refTable, List<String> refColumns, String onDelete) {
			this.name = name;
			this.columns = columns;
			this.refTable = refTable;
			this.refColumns = refColumns;
			this.onDelete = onDelete;
		}

		public String getName() {
			return name;
		}

		public List<String> getColumns() {
			return columns;
		}

		public String getRefTable() {
			return refTable;
		}

		public List<String> getRefColumns() {
			return refColumns;
		}

		public String getOnDelete() {
			return onDelete;
		}
	}
}
