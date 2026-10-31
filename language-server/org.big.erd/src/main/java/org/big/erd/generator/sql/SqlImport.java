package org.big.erd.generator.sql;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.big.erd.entityRelationship.Model;
import org.big.erd.generator.IErGenerator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.generator.IFileSystemAccess2;
import org.eclipse.xtext.generator.IGeneratorContext;
import org.eclipse.xtext.util.RuntimeIOException;
import org.eclipse.xtext.xbase.lib.Exceptions;

/**
 * Generates an ER model from SQL DDL statements.
 * 
 * TODO: Integrate SQL Import
 */
public class SqlImport implements IErGenerator {

	private static final boolean REPLACE_NEWLINES = false;

	private static final String SIZE_BASE_PATTERN = "\\d+(?:,\\s*\\d+)?";
	private static final String SIZE_PATTERN = "\\(((?:" + SIZE_BASE_PATTERN + ")|\\*)";
	private static final int SIZE_VALUE = 1;
	
	private static final String ATTRIBUTE_PATTERN = "\\s*([^\\)\\s,]*)(?: (.*\\([\\d\\*]+.*?\\)|[^,\\s]+))?([^,\\)]*),?\\s*(?:--(.*))?";
	private static final int ATTRIBUTE_NAME = 1;
	private static final int ATTRIBUTE_TYPE = 2;
	private static final int ATTRIBUTE_KEYWORDS = 3;
	private static final int ATTRIBUTE_COMMENT = 4;
	
	private static final String FOREIGN_KEY_BASE_PATTERN = "FOREIGN KEY\\s*\\((.*?)\\)\\s*REFERENCES (?:.+?\\.)?(\\S+)\\s*\\((.*?)\\)";
	private static final String FOREIGN_KEY_PATTERN = ".*?(?:CONSTRAINT\\s+(\\S+)\\s+)?" + FOREIGN_KEY_BASE_PATTERN + "([^,]*?)";
	private static final int FOREIGN_KEY_NAME = 1;
	private static final int FOREIGN_KEY_ATTRIBUTES = 2;
	private static final int FOREIGN_KEY_REF_TABLE = 3;

	private static final String PRIMARY_KEY_BASE_PATTERN = "PRIMARY KEY(?: CLUSTERED)?\\s*\\((.*?)\\)";
	private static final String PRIMARY_KEY_PATTERN = ".*" + PRIMARY_KEY_BASE_PATTERN + "(?:WITH.+?\\(.*?\\))?[^,\\)]*?";
	// UNIQUE (a, b), CONSTRAINT name UNIQUE (a, b) or UNIQUE KEY name (a, b)
	private static final String UNIQUE_BASE_PATTERN = "\\s*(?:CONSTRAINT\\s+\\S+\\s+)?UNIQUE(?:\\s+KEY)?(?:\\s+[^\\s\\(]+)?\\s*\\(([^)]*)\\)[^,\\r\\n]*";
	private static final int UNIQUE_ATTRIBUTES = 1;

	@SuppressWarnings("unused")
	// for debugging
	private static final String CREATE_TABLE_PATTERN_DEBUG = getCreateTablePattern(false);
	private static final String CREATE_TABLE_PATTERN = getCreateTablePattern(true);
	private static final int CREATE_TABLE_NAME = 1;
	private static final int CREATE_TABLE_ATTRIBUTES = 2;
	private static final int CREATE_TABLE_PRIMARY_KEY = 3;
	private static final int CREATE_TABLE_UNIQUE_KEYS = 4;
	private static final int CREATE_TABLE_FOREIGN_KEYS = 5;

	@SuppressWarnings("unused")
	// for debugging
	private static final String ALTER_TABLE_PATTERN_DEBUG = getAlterTablePattern(false);
	private static final String ALTER_TABLE_PATTERN = getAlterTablePattern(true);
	private static final int ALTER_TABLE_NAME = 1;
	private static final int ALTER_TABLE_PRIMARY_KEY = 2;
	private static final int ALTER_TABLE_FOREIGN_KEYS = 3;

	private static String getCreateTablePattern(boolean replace) {
		String pattern = "CREATE TABLE(?: IF NOT EXISTS)? (?:.+?\\.)?(\\S+)\\s*\\(((?:\r\n"
				+ replaceCaptureGroups(ATTRIBUTE_PATTERN, replace) + ")*?)(?:\r\n"
				+ PRIMARY_KEY_PATTERN + ")?((?:,\r\n"
				+ replaceCaptureGroups(UNIQUE_BASE_PATTERN, replace) + ")*)((?:,\r\n"
				+ replaceCaptureGroups(FOREIGN_KEY_PATTERN, replace) + ")*)\r?\n?"
				+ "\\s*\\)";
		if (replace) {
			pattern = removeNewlines(pattern, REPLACE_NEWLINES);
		}
		return pattern;
	}

	private static String getAlterTablePattern(boolean replace) {
		String pattern = "ALTER TABLE(?: ONLY)?(?: IF EXISTS)? (?:.+?\\.)?(\\S+)\\s*(?:\r?\n?"
				+ PRIMARY_KEY_PATTERN + ")?(\r?\n?"
				+ replaceCaptureGroups(FOREIGN_KEY_PATTERN, replace) + ")?;?";
		if (replace) {
			pattern = removeNewlines(pattern, REPLACE_NEWLINES);
		}
		return pattern;
	}
	
	private static String replaceCaptureGroups(String pattern, boolean replace) {
		if (replace) {
			// replace the capturing groups (not escaped leading parenthesis, without ?) with non-capturing groups
			return pattern.replaceAll("(?<!\\\\)\\((?!\\?)", "(?:");
		}
		return pattern;
	}

	@Override
	public void generate(final Resource resource, final IFileSystemAccess2 fsa, final IGeneratorContext context) {
		final Model diagram = (Model) resource.getContents().get(0);
		String diagramName = diagram.getName();
		final String fileName = (diagramName != null ? diagramName : "output") + ".sql";
		final String fileNameDrop = (diagramName != null ? diagramName : "output") + ".erd";
		try {
			String text = fsa.readTextFile(fileName).toString();
			StringConcatenation fileContent = generateFileContent(diagramName, text);
			fsa.generateFile(fileNameDrop, fileContent);
		} catch (final Throwable t) {
			if (t instanceof RuntimeIOException) {
				throw new Error("Could not generate file. Did you open a folder?", t);
			} else {
				throw Exceptions.sneakyThrow(t);
			}
		}
	}
	
	/** Converts SQL DDL statements to an ER model in textual notation. */
	public String importSql(String diagramName, String sql) {
		return generateFileContent(diagramName, sql).toString();
	}

	public static boolean handleFiles() throws IOException {
		boolean success = true;
		String[] fileNames = new String[] {"biger", "biger-basic", "biger-relships", "db2look", "mssql", "mysql", "oracle", "pgadmin", "pgdump"};
		for (String fileName : fileNames) {
			fileName = fileName + ".txt";
			File file = new File("src\\test\\resources\\org\\big\\erd\\generator\\sql\\import\\input", fileName);
			try (InputStream fis = SqlImport.class.getResourceAsStream("import/input/" + fileName)) {
				if (fis != null) {
					byte[] content = fis.readAllBytes();
					String strContent = new String(content);
					success &= importNotation(file, strContent, "default_notation", new SqlImport());
					success &= importNotation(file, strContent, "uml", new UmlSqlImport());
					success &= importNotation(file, strContent, "min_max", new MinMaxSqlImport());
					success &= importNotation(file, strContent, "crows_foot", new CrowsFootSqlImport());
					success &= importNotation(file, strContent, "chen", new ChenSqlImport());
					success &= importNotation(file, strContent, "bachman", new BachmanSqlImport());
				} else {
					System.err.println("could not open file: " + "import/input/" + fileName);
				}
			}
		}
		return success;
	}

	private static boolean importNotation(File file, String strContent, String importKey, SqlImport importObject) throws IOException, FileNotFoundException {
		StringConcatenation fileContent = importObject.generateFileContent("test", strContent);
		String outputContent = fileContent.toString();
		File output = new File(new File(new File(file.getParentFile().getParentFile(), "output"), importKey), file.getName());
		if (output.getParentFile().isDirectory()) {
			try (FileOutputStream fos = new FileOutputStream(output)) {
				fos.write(outputContent.getBytes());
			}
		}
		try (InputStream fisExpected = SqlImport.class.getResourceAsStream("import/output/expected/" + importKey + "/" + file.getName())) {
			byte[] contentExpected = fisExpected.readAllBytes();
			if (!new String(contentExpected).equals(outputContent)) {
				System.err.println("unexpected output in file: " + output.getAbsolutePath());
				return false;
			}
		}
		return true;
	}

	/** A foreign key constraint: columns of the table that reference a key of another table. */
	private static class ForeignKeyGroup {
		String name;
		List<String> attributes;
		String refTable;
	}

	/** Information collected for each table. */
	private static class TableInfo {
		String name;
		/** attributes that are not part of a foreign key */
		List<SqlAttribute> attributes;
		Map<String, SqlAttribute> attributesAll = new HashMap<>();
		List<String> primaryKey;
		List<List<String>> uniqueKeys = new ArrayList<>();
		List<ForeignKeyGroup> foreignKeys = new ArrayList<>();
		boolean entity;
		boolean weak;
	}

	private StringConcatenation generateFileContent(String diagramName, String text) {
		String preprocessedSql = preprocessSql(text);

		StringConcatenation fileContent = new StringConcatenation();
		fileContent.append("// ER Model");
		fileContent.newLineIfNotEmpty();
		fileContent.append("erdiagram ");
		fileContent.append(diagramName);
		fileContent.newLineIfNotEmpty();
		fileContent.append("// Options");
		fileContent.newLineIfNotEmpty();
		fileContent.append("notation=");
		fileContent.append(getNotation());
		fileContent.newLineIfNotEmpty();

		Map<String, String> presetPrimaryKeys = new LinkedHashMap<>();
		Map<String, String> presetForeignKeys = new LinkedHashMap<>();
		Map<String, TableInfo> tables = new LinkedHashMap<>();

		// collect primary and foreign keys
		Pattern pPreset = Pattern.compile(ALTER_TABLE_PATTERN, Pattern.CASE_INSENSITIVE);
		Matcher mPreset = pPreset.matcher(preprocessedSql);
		while (mPreset.find()) {
			String tableName = mPreset.group(ALTER_TABLE_NAME);
			String tablePrimaryKey = mPreset.group(ALTER_TABLE_PRIMARY_KEY);
			String tableForeignKeys = mPreset.group(ALTER_TABLE_FOREIGN_KEYS);

			if (tablePrimaryKey != null) {
				presetPrimaryKeys.put(tableName, tablePrimaryKey);
			}
			if (tableForeignKeys != null) {
				if (presetForeignKeys.containsKey(tableName)) {
					presetForeignKeys.put(tableName, presetForeignKeys.get(tableName) + "," + tableForeignKeys);
				} else {
					presetForeignKeys.put(tableName, tableForeignKeys);
				}
			}
		}

		fileContent.append("// Entities");
		fileContent.newLineIfNotEmpty();

		// process tables
		Pattern p = Pattern.compile(CREATE_TABLE_PATTERN, Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(preprocessedSql);
		while (m.find()) {
			TableInfo table = new TableInfo();
			String tableName = m.group(CREATE_TABLE_NAME);
			String tableAttributes = m.group(CREATE_TABLE_ATTRIBUTES);
			String tablePrimaryKey = m.group(CREATE_TABLE_PRIMARY_KEY);
			String tableUniqueKeys = m.group(CREATE_TABLE_UNIQUE_KEYS);
			String tableForeignKeys = m.group(CREATE_TABLE_FOREIGN_KEYS);
			table.name = tableName;

			// process foreign keys
			Set<String> foreignKeyAttributes = new HashSet<>();
			if (tableForeignKeys == null || tableForeignKeys.isEmpty()) {
				tableForeignKeys = presetForeignKeys.get(tableName);
			}
			if (tableForeignKeys != null) {
				Pattern pFor = Pattern.compile(FOREIGN_KEY_PATTERN, Pattern.CASE_INSENSITIVE);
				Matcher mFor = pFor.matcher(tableForeignKeys);
				while (mFor.find()) {
					ForeignKeyGroup foreignKey = new ForeignKeyGroup();
					foreignKey.name = mFor.group(FOREIGN_KEY_NAME);
					foreignKey.attributes = splitAndTrim(mFor.group(FOREIGN_KEY_ATTRIBUTES));
					foreignKey.refTable = mFor.group(FOREIGN_KEY_REF_TABLE);
					table.foreignKeys.add(foreignKey);
					foreignKeyAttributes.addAll(foreignKey.attributes);
				}
			}

			// process primary and unique keys
			if (tablePrimaryKey == null) {
				tablePrimaryKey = presetPrimaryKeys.get(tableName);
			}
			List<String> primaryKeyAttributes = splitKeyAttributes(tablePrimaryKey);
			table.primaryKey = primaryKeyAttributes;
			if (tableUniqueKeys != null) {
				Matcher mUnique = Pattern.compile(UNIQUE_BASE_PATTERN, Pattern.CASE_INSENSITIVE).matcher(tableUniqueKeys);
				while (mUnique.find()) {
					table.uniqueKeys.add(splitKeyAttributes(mUnique.group(UNIQUE_ATTRIBUTES)));
				}
			}

			// process attributes
			List<SqlAttribute> attributes = new ArrayList<>();
			Pattern pAtt = Pattern.compile(ATTRIBUTE_PATTERN, Pattern.CASE_INSENSITIVE);
			Matcher mAtt = pAtt.matcher(tableAttributes);
			boolean weak = false;
			boolean isEntity = false;
			while (mAtt.find()) {
				String attributeName = mAtt.group(ATTRIBUTE_NAME);
				String attributeType = mAtt.group(ATTRIBUTE_TYPE);
				String attributeKeywords = mAtt.group(ATTRIBUTE_KEYWORDS);
				String attributeComment = mAtt.group(ATTRIBUTE_COMMENT);

				if (!attributeName.isBlank()) {
					int size = 0;
					Integer precision = null;
					if (attributeType != null) {
						Pattern pSize = Pattern.compile(SIZE_PATTERN);
						Matcher mSize = pSize.matcher(attributeType);
						if (mSize.find()) {
							String sizeValue = mSize.group(SIZE_VALUE);
							if ("*".equals(sizeValue)) {
								size = -1;
							} else {
								int index = sizeValue.indexOf(',');
								if (index > 0) {
									precision = Integer.parseInt(sizeValue.substring(index + 1).trim());
									sizeValue = sizeValue.substring(0, index);
								}
								size = Integer.parseInt(sizeValue);
							}
						}
					}
					if (size > 0) {
						String strPrecision = "";
						if (precision != null) {
							strPrecision = ", " + precision;
						}
						attributeType = replaceSpaces(deQuote(attributeType.substring(0, attributeType.indexOf("(" + size)))) + "(" + size + strPrecision + ")";
					} else if (size < 0) {
						attributeType = replaceSpaces(deQuote(attributeType.substring(0, attributeType.indexOf("(*"))));
					}
					SqlAttribute attribute = new SqlAttribute();
					attribute.setAttributeName(attributeName);
					attribute.setAttributeType(attributeType);
					attribute.setAttributeKeywords(attributeKeywords);
					attribute.setAttributeComment(attributeComment);

					table.attributesAll.put(attributeName, attribute);

					if (!foreignKeyAttributes.contains(attributeName)) {
						attributes.add(attribute);

						if (primaryKeyAttributes.contains(attributeName) || foreignKeyAttributes.isEmpty()) {
							isEntity = true;
						}
					} else if (primaryKeyAttributes.contains(attributeName)) {
						weak = true;
					}
				}
			}

			if (isEntity) {
				// generate entities
				if (weak) {
					fileContent.append("weak ");
				}
				fileContent.append("entity ");
				fileContent.append(capitalize(deQuote(tableName)));
				fileContent.append(" {");
				fileContent.newLineIfNotEmpty();

				addAttributes(fileContent, attributes, primaryKeyAttributes, weak);

				fileContent.append("}");
				fileContent.newLineIfNotEmpty();
			}
			table.attributes = attributes;
			table.entity = isEntity;
			table.weak = weak && isEntity;
			tables.put(tableName, table);
		}

		fileContent.append("// Relationships");
		fileContent.newLineIfNotEmpty();

		// generate relationships
		Set<String> usedNames = new HashSet<>();
		for (String tableName : tables.keySet()) {
			usedNames.add(capitalize(deQuote(tableName)));
		}
		for (TableInfo table : tables.values()) {
			if (table.foreignKeys.isEmpty()) {
				continue;
			}
			if (table.entity) {
				// foreign keys that are part of the primary key identify a weak entity,
				// all others represent 1:1 or 1:N relationships
				List<ForeignKeyGroup> identifying = new ArrayList<>();
				for (ForeignKeyGroup foreignKey : table.foreignKeys) {
					boolean inPrimaryKey = false;
					for (String attribute : foreignKey.attributes) {
						inPrimaryKey |= table.primaryKey.contains(attribute);
					}
					if (inPrimaryKey) {
						identifying.add(foreignKey);
					} else {
						addForeignKeyRelationship(fileContent, table, foreignKey, usedNames);
					}
				}
				if (table.weak && !identifying.isEmpty()) {
					addWeakRelationship(fileContent, table, identifying, usedNames);
				}
			} else {
				addRelationshipTable(fileContent, table);
			}
		}
		return fileContent;
	}

	/** Weak relationship between the strong entities and the weak entity stored in {@code table}. */
	private void addWeakRelationship(StringConcatenation fileContent, TableInfo table, List<ForeignKeyGroup> identifying, Set<String> usedNames) {
		Set<String> refTables = new LinkedHashSet<>();
		for (ForeignKeyGroup foreignKey : identifying) {
			refTables.add(foreignKey.refTable);
		}
		refTables.add(table.name);
		StringBuilder sb = new StringBuilder();
		for (String refTable : refTables) {
			sb.append(capitalize(deQuote(refTable)));
		}
		fileContent.append("weak relationship ");
		fileContent.append(GeneratorUtils.findUniqueName(sb.toString(), usedNames));
		fileContent.append(" {");
		fileContent.newLineIfNotEmpty();

		boolean first = true;
		int countMultiple = 0;
		for (ForeignKeyGroup foreignKey : identifying) {
			fileContent.append(first ? "\t" : " -> ");
			fileContent.append(capitalize(deQuote(foreignKey.refTable)));
			fileContent.append("[");
			fileContent.append(getCardinality(true, first, countMultiple));
			fileContent.append("]");
			if (!first) {
				countMultiple++;
			}
			first = false;
		}
		fileContent.append(" -> ");
		fileContent.append(capitalize(deQuote(table.name)));
		fileContent.append("[");
		fileContent.append(getCardinality(false, false, countMultiple));
		fileContent.append("]");
		fileContent.newLineIfNotEmpty();
		fileContent.append("}");
		fileContent.newLineIfNotEmpty();
	}

	/**
	 * 1:1 or 1:N relationship stored as foreign key in an entity table.
	 * The referenced side is [1] if the columns are NOT NULL, otherwise [0..1].
	 * The referencing side is [0..1] if the columns are unique, otherwise [N].
	 */
	private void addForeignKeyRelationship(StringConcatenation fileContent, TableInfo table, ForeignKeyGroup foreignKey, Set<String> usedNames) {
		String name = foreignKey.name != null ? deQuote(foreignKey.name) : null;
		if (name != null && name.length() > 3 && name.toLowerCase().startsWith("fk_")) {
			// constraint names generated by bigER: fk_<relationship>
			name = name.substring(3);
		} else {
			name = capitalize(deQuote(table.name)) + capitalize(deQuote(foreignKey.refTable));
		}
		boolean mandatory = isMandatory(table, foreignKey.attributes);
		boolean unique = isUnique(table, foreignKey.attributes);

		fileContent.append("relationship ");
		fileContent.append(GeneratorUtils.findUniqueName(name, usedNames));
		fileContent.append(" {");
		fileContent.newLineIfNotEmpty();
		fileContent.append("\t");
		fileContent.append(capitalize(deQuote(foreignKey.refTable)));
		fileContent.append("[");
		fileContent.append(getCardinality(mandatory, true, 0));
		fileContent.append("] -> ");
		fileContent.append(capitalize(deQuote(table.name)));
		fileContent.append("[");
		fileContent.append(getCardinality(false, unique, unique ? 0 : 1));
		fileContent.append("]");
		fileContent.newLineIfNotEmpty();
		fileContent.append("}");
		fileContent.newLineIfNotEmpty();
	}

	/**
	 * Relationship stored in its own table. A participant has a maximum cardinality of 1
	 * if a primary or unique key of the table does not contain its columns.
	 */
	private void addRelationshipTable(StringConcatenation fileContent, TableInfo table) {
		Set<String> participantAttributes = new HashSet<>();
		for (ForeignKeyGroup foreignKey : table.foreignKeys) {
			participantAttributes.addAll(foreignKey.attributes);
		}
		List<List<String>> candidateKeys = new ArrayList<>();
		if (!table.primaryKey.isEmpty()) {
			candidateKeys.add(table.primaryKey);
		}
		candidateKeys.addAll(table.uniqueKeys);

		fileContent.append("relationship ");
		fileContent.append(capitalize(deQuote(table.name)));
		fileContent.append(" {");
		fileContent.newLineIfNotEmpty();

		boolean first = true;
		boolean anySingle = false;
		int countMultiple = 0;
		for (ForeignKeyGroup foreignKey : table.foreignKeys) {
			boolean single = false;
			for (List<String> key : candidateKeys) {
				if (!key.isEmpty() && participantAttributes.containsAll(key) && Collections.disjoint(key, foreignKey.attributes)) {
					single = true;
				}
			}
			fileContent.append(first ? "\t" : " -> ");
			fileContent.append(capitalize(deQuote(foreignKey.refTable)));
			fileContent.append("[");
			fileContent.append(getCardinality(isMandatory(table, foreignKey.attributes), single, countMultiple));
			fileContent.append("]");
			if (!single) {
				countMultiple++;
			}
			anySingle |= single;
			first = false;
		}
		if (!anySingle) {
			fileContent.append("\t");
			fileContent.append("// example cardinalities");
		}
		fileContent.newLineIfNotEmpty();
		addAttributes(fileContent, table.attributes, null, false);
		fileContent.append("}");
		fileContent.newLineIfNotEmpty();
	}

	private boolean isMandatory(TableInfo table, List<String> attributes) {
		for (String attribute : attributes) {
			SqlAttribute sqlAttribute = table.attributesAll.get(attribute);
			if (sqlAttribute == null || !sqlAttribute.isMandatory()) {
				return false;
			}
		}
		return !attributes.isEmpty();
	}

	private boolean isUnique(TableInfo table, List<String> attributes) {
		Set<String> attributeSet = new HashSet<>(attributes);
		if (attributeSet.equals(new HashSet<>(table.primaryKey))) {
			return true;
		}
		for (List<String> unique : table.uniqueKeys) {
			if (attributeSet.equals(new HashSet<>(unique))) {
				return true;
			}
		}
		return false;
	}

	/** Splits a key definition such as "a, b DESC" into attribute names. */
	private List<String> splitKeyAttributes(String key) {
		List<String> attributes = new ArrayList<>();
		if (key != null) {
			for (String id : key.split(",")) {
				id = id.trim();
				int index = id.indexOf(" ");
				if (index > 0) {
					id = id.substring(0, index);
				}
				if (!id.isEmpty()) {
					attributes.add(id);
				}
			}
		}
		return attributes;
	}

	protected String getNotation() {
		return "default";
	}

	protected String getCardinality(boolean isMandatory, boolean isSingle, int countMultiple) {
		if (isSingle) {
			return isMandatory ? "1" : "0..1";
		}
		return "N";
	}
	
	// ER model does not support spaces in data types
	private String replaceSpaces(String value) {
		return value.replace(" ", "_");
	}

	private String preprocessSql(String text) {
		// the patterns expect Windows line endings
		text = text.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\r\n");
		// Oracle: insert line break
		text = text.replace("(\t", "(\r\n\t");
		// MsSql: combine primary key and foreign key clauses into a single line
		Pattern pPrimaryKey = Pattern.compile(PRIMARY_KEY_BASE_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher mPrimaryKey = pPrimaryKey.matcher(text);
		while (mPrimaryKey.find()) {
			String originalText = mPrimaryKey.group();
			String replacedText = removeNewlines(originalText);
			text = text.replace(originalText, replacedText);
		}
		Pattern pForeignKey = Pattern.compile(FOREIGN_KEY_BASE_PATTERN, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		Matcher mForeignKey = pForeignKey.matcher(text);
		while (mForeignKey.find()) {
			String originalText = mForeignKey.group();
			String replacedText = removeNewlines(originalText);
			text = text.replace(originalText, replacedText);
		}
		return removeNewlines(text, REPLACE_NEWLINES);
	}

	private void addAttributes(StringConcatenation fileContent, List<SqlAttribute> attributes, List<String> primaryKeyAttributes, boolean weak) {
		if (attributes != null) {
			for (SqlAttribute attribute : attributes) {
				fileContent.append("\t");
				fileContent.append(deQuote(attribute.getAttributeName()));
				if (attribute.getAttributeType() != null) {
					fileContent.append(": ");
					fileContent.append(deQuote(attribute.getAttributeType()));
				}
				if (primaryKeyAttributes != null && primaryKeyAttributes.contains(attribute.getAttributeName())) {
					fileContent.append(" ");
					if (weak) {
						fileContent.append("partial-");
					}
					fileContent.append("key");
				} else if (!attribute.isMandatory()) {
					fileContent.append(" optional");
				}
				if (attribute.getAttributeComment() != null) {
					fileContent.append("\t");
					fileContent.append("//");
					fileContent.append(attribute.getAttributeComment());
				}
				fileContent.newLineIfNotEmpty();
			}
		}
	}

	private List<String> splitAndTrim(String attributes) {
		String[] arr = attributes.split(",");
		List<String> attributeList = new ArrayList<>();
		for (String id : arr) {
			attributeList.add(id.trim());
		}
		return attributeList;
	}
	
	private String capitalize(String str) {
		if (str != null && !str.isEmpty()) {
			str = str.substring(0, 1).toUpperCase() + str.substring(1);
		}
		return str;
	}
	
	private String deQuote(String str) {
		str = deQuote(str, "'");
		str = deQuote(str, "\"");
		str = deQuote(str, "`");
		str = deQuote(str, "[", "]");
		return str;
	}
	
	private String deQuote(String str, String quoteChar) {
		return deQuote(str, quoteChar, null);
	}
	
	private String deQuote(String str, String quoteChar, String differentEndChar) {
		if (str != null && str.length() > 1) {
			if (differentEndChar == null) {
				if (str.startsWith(quoteChar) && str.endsWith(quoteChar)) {
					str = str.substring(1, str.length() - 1);
				}
			} else {
				if (str.startsWith(quoteChar) && str.endsWith(differentEndChar)) {
					str = str.substring(1, str.length() - 1);
				}
			}
		}
		return str;
	}
	
	private static String removeNewlines(String str) {
		return removeNewlines(str, true);
	}
	
	private static String removeNewlines(String str, boolean replace) {
		if (replace) {
			str = removeFromString(str, "\r\n");
			str = removeFromString(str, "\r");
			str = removeFromString(str, "\n");
		}
		return str;
	}
	
	private static String removeFromString(String str, String search) {
		if (str != null) {
			return str.replace(search, "");
		}
		return str;
	}
}
