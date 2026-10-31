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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.big.erd.entityRelationship.Attribute;
import org.big.erd.entityRelationship.AttributeType;
import org.big.erd.entityRelationship.CardinalityType;
import org.big.erd.entityRelationship.Entity;
import org.big.erd.entityRelationship.Model;
import org.big.erd.entityRelationship.RelationEntity;
import org.big.erd.entityRelationship.Relationship;
import org.big.erd.generator.IErGenerator;
import org.big.erd.generator.sql.SqlSchema.Column;
import org.big.erd.generator.sql.SqlSchema.ForeignKey;
import org.big.erd.generator.sql.SqlSchema.Table;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.generator.IFileSystemAccess2;
import org.eclipse.xtext.generator.IGeneratorContext;

/**
 * Generates vendor-agnostic SQL from the ER model.
 * Can be extended to provide vendor-specific dialects.
 *
 * Generation happens in two steps: the ER model is first mapped to a {@link SqlSchema},
 * which is then rendered as SQL text.
 */
public class SqlGenerator implements IErGenerator {

	public static boolean handleFiles(Function<CharSequence, Model> parseFunction) throws IOException {
		boolean success = true;
		String[] fileNames = new String[] {"biger", "biger-basic", "biger-relships"};
		for (String fileName : fileNames) {
			fileName = fileName + ".txt";
			File file = new File("src\\test\\resources\\org\\big\\erd\\generator\\sql\\export\\input", fileName);
			try (InputStream fis = SqlGenerator.class.getResourceAsStream("export/input/" + fileName)) {
				if (fis != null) {
					byte[] content = fis.readAllBytes();
					String strContent = new String(content);
					Model model = parseFunction.apply(strContent);
					success &= exportVendor(file, model, "default_generator", false, new SqlGenerator());
					success &= exportVendor(file, model, "db2", false, new Db2Generator());
					success &= exportVendor(file, model, "mssql", false, new MsSqlGenerator());
					success &= exportVendor(file, model, "mysql", false, new MySqlGenerator());
					success &= exportVendor(file, model, "oracle", false, new OracleGenerator());
					success &= exportVendor(file, model, "postgres", false, new PostgresGenerator());
					success &= exportVendor(file, model, "default_generator_drop", true, new SqlGenerator());
					success &= exportVendor(file, model, "db2_drop", true, new Db2Generator());
					success &= exportVendor(file, model, "mssql_drop", true, new MsSqlGenerator());
					success &= exportVendor(file, model, "mysql_drop", true, new MySqlGenerator());
					success &= exportVendor(file, model, "oracle_drop", true, new OracleGenerator());
					success &= exportVendor(file, model, "postgres_drop", true, new PostgresGenerator());
				} else {
					System.err.println("could not open file: " + "export/input/" + fileName);
				}
			}
		}
		return success;
	}

	private static boolean exportVendor(File file, Model model, String exportKey, boolean drop, SqlGenerator exportObject) throws IOException, FileNotFoundException {
		StringConcatenation fileContent = exportObject.generateFileContent(model, drop);
		String outputContent = fileContent.toString();
		File output = new File(new File(new File(file.getParentFile().getParentFile(), "output"), exportKey), file.getName());
		if (output.getParentFile().isDirectory()) {
			try (FileOutputStream fos = new FileOutputStream(output)) {
				fos.write(outputContent.getBytes());
			}
		}
		try (InputStream fisExpected = SqlImport.class.getResourceAsStream("export/output/expected/" + exportKey + "/" + file.getName())) {
			byte[] contentExpected = fisExpected.readAllBytes();
			if (!new String(contentExpected).equals(outputContent)) {
				System.err.println("unexpected output in file: " + output.getAbsolutePath());
				return false;
			}
		}
		return true;
	}

	@Override
	public void generate(final Resource resource, final IFileSystemAccess2 fsa, final IGeneratorContext context) {
		final Model diagram = (Model) resource.getContents().get(0);
		String diagramName = diagram.getName();
		final String fileName = (diagramName != null ? diagramName : "output") + ".sql";
		StringConcatenation fileContent = generateFileContent(diagram, false);
		fsa.generateFile(fileName, fileContent);
	}

	public void generateDrop(final Resource resource, final IFileSystemAccess2 fsa, final IGeneratorContext context) {
		final Model diagram = (Model) resource.getContents().get(0);
		String diagramName = diagram.getName();
		final String fileNameDrop = (diagramName != null ? diagramName : "output") + "-drop.sql";
		StringConcatenation fileContentDrop = generateFileContent(diagram, true);
		fsa.generateFile(fileNameDrop, fileContentDrop);
	}

	public String generate(final Model diagram) {
		return generateFileContent(diagram, false).toString();
	}

	public String generateDrop(final Model diagram) {
		return generateFileContent(diagram, true).toString();
	}

	private StringConcatenation generateFileContent(final Model diagram, boolean drop) {
		SqlSchema schema = buildSchema(diagram);
		return drop ? renderDrop(schema) : renderCreate(schema);
	}

	// ---------------------------------------------------------------------
	// Step 1: map the ER model to a relational schema
	//
	// Cardinalities are read "across": in A[1] -> B[N], [1] means every B is
	// related to exactly one A, [N] means an A is related to any number of Bs.
	// [N] and [1..N] are treated as "zero or more" (no minimum is enforced).
	// ---------------------------------------------------------------------

	private static final String ON_DELETE_CASCADE = "CASCADE";
	private static final String ON_DELETE_SET_NULL = "SET NULL";

	/** A column of a table, before it is added to the schema. */
	private static class KeyColumn {
		final String name;
		final Attribute attribute;
		final String renamedFrom;

		KeyColumn(String name, Attribute attribute, String renamedFrom) {
			this.name = name;
			this.attribute = attribute;
			this.renamedFrom = renamedFrom;
		}
	}

	/** State of a single schema build. */
	private static class BuildContext {
		final SqlSchema schema = new SqlSchema();
		/** effective primary key per entity name (the entity's table may store the key under renamed columns) */
		final Map<String, List<KeyColumn>> keys = new HashMap<>();

		List<KeyColumn> keyOf(Entity entity) {
			List<KeyColumn> key = keys.get(entity.getName());
			if (key == null) {
				throw new IllegalArgumentException("Entity " + entity.getName() + " not yet processed.");
			}
			return key;
		}
	}

	public SqlSchema buildSchema(final Model diagram) {
		BuildContext ctx = new BuildContext();
		for (final Entity entity : diagram.getEntities()) {
			if (!entity.isWeak()) {
				buildEntityTable(ctx, entity);
			}
		}
		for (final Relationship relationship : diagram.getRelationships()) {
			if (relationship.isWeak()) {
				buildWeakEntityTable(ctx, relationship);
			}
		}
		for (final Relationship relationship : diagram.getRelationships()) {
			if (!relationship.isWeak()) {
				buildRelationship(ctx, relationship);
			}
		}
		return ctx.schema;
	}

	private void buildEntityTable(BuildContext ctx, Entity entity) {
		Table table = ctx.schema.addTable(entity.getName());
		Set<String> usedNames = new HashSet<>();
		List<KeyColumn> attributes = deduplicate(entity.getAttributes(), usedNames);
		addColumns(table, attributes);
		List<KeyColumn> key = filterByType(attributes, AttributeType.KEY);
		setPrimaryKey(ctx, table, entity.getName(), Collections.singletonList(key));
	}

	private void buildWeakEntityTable(BuildContext ctx, Relationship relationship) {
		final RelationEntity strongParticipant = this.getStrongParticipant(relationship);
		final Entity strong = strongParticipant.getTarget();
		final Entity weak = this.getWeakEntity(relationship);
		Table table = ctx.schema.addTable(weak.getName());

		Set<String> usedNames = new HashSet<>();
		List<KeyColumn> weakAttributes = deduplicate(weak.getAttributes(), usedNames);
		List<KeyColumn> relationshipAttributes = deduplicate(relationship.getAttributes(), usedNames);
		List<KeyColumn> strongKey = ctx.keyOf(strong);
		List<KeyColumn> strongKeyColumns = referencingColumns(strongParticipant, strongKey, usedNames);

		addColumns(table, weakAttributes);
		addColumns(table, relationshipAttributes);
		addColumns(table, strongKeyColumns);
		setPrimaryKey(ctx, table, weak.getName(), List.of(filterByType(weakAttributes, AttributeType.PARTIAL_KEY), strongKeyColumns));
		addForeignKey(table, foreignKeyName(table, strongKeyColumns), strongKeyColumns, strong.getName(), strongKey, ON_DELETE_CASCADE);
	}

	private void buildRelationship(BuildContext ctx, Relationship relationship) {
		List<RelationEntity> participants = getParticipants(relationship);
		boolean complete = !participants.isEmpty();
		for (RelationEntity participant : participants) {
			complete &= participant.getTarget() != null;
		}
		if (!complete || participants.size() != 2) {
			// ternary (or incomplete) relationships always get their own table
			buildRelationshipTable(ctx, relationship, participants);
			return;
		}

		RelationEntity first = participants.get(0);
		RelationEntity second = participants.get(1);
		CardinalityType c1 = first.getCardinality();
		CardinalityType c2 = second.getCardinality();
		if (!isSingle(c1) && !isSingle(c2) || isUnspecified(c1) || isUnspecified(c2)) {
			// many-to-many, or cardinalities not given: own table
			buildRelationshipTable(ctx, relationship, participants);
			return;
		}

		// 1:N or 1:1. The "holder" side stores the reference to the "referenced" side.
		boolean oneToOne = isSingle(c1) && isSingle(c2);
		RelationEntity holder;
		RelationEntity referenced;
		if (oneToOne) {
			// put the reference on the side whose partner is [1], so that it can be NOT NULL
			boolean secondHolds = c1 == CardinalityType.ONE || c2 != CardinalityType.ONE;
			holder = secondHolds ? second : first;
			referenced = secondHolds ? first : second;
		} else {
			holder = isSingle(c1) ? second : first;
			referenced = isSingle(c1) ? first : second;
		}

		if (relationship.getAttributes().isEmpty()) {
			buildForeignKeyMapping(ctx, relationship, holder, referenced, oneToOne);
		} else {
			buildRelationshipTable(ctx, relationship, holder, referenced, oneToOne);
		}
	}

	/**
	 * Maps a 1:N or 1:1 relationship without attributes to foreign key columns in the holder's table.
	 */
	private void buildForeignKeyMapping(BuildContext ctx, Relationship relationship, RelationEntity holder, RelationEntity referenced, boolean oneToOne) {
		Entity holderEntity = holder.getTarget();
		Entity referencedEntity = referenced.getTarget();
		ctx.keyOf(holderEntity);
		List<KeyColumn> referencedKey = ctx.keyOf(referencedEntity);
		Table table = ctx.schema.getTable(holderEntity.getName());

		Set<String> usedNames = new HashSet<>(table.getColumnNames());
		List<KeyColumn> columns = referencingColumns(referenced, referencedKey, usedNames);
		boolean mandatory = referenced.getCardinality() == CardinalityType.ONE;
		for (KeyColumn column : columns) {
			Column added = addColumn(table, column);
			if (added != null) {
				added.setNullable(!mandatory);
			}
		}
		List<String> columnNames = names(columns);
		if (oneToOne && !columnNames.isEmpty()) {
			table.getUniqueKeys().add(columnNames);
		}
		addForeignKey(table, "fk_" + relationship.getName(), columns, referencedEntity.getName(), referencedKey,
				mandatory ? null : ON_DELETE_SET_NULL);

		if (oneToOne && holder.getCardinality() == CardinalityType.ONE) {
			table.getNotes().add(minimumNotEnforced(relationship, referencedEntity, holderEntity));
		}
	}

	/**
	 * Maps a 1:N or 1:1 relationship with attributes to its own table.
	 * Each holder appears at most once in that table.
	 */
	private void buildRelationshipTable(BuildContext ctx, Relationship relationship, RelationEntity holder, RelationEntity referenced, boolean oneToOne) {
		List<RelationEntity> participants = getParticipants(relationship);
		Table table = ctx.schema.addTable(relationship.getName());
		Set<String> usedNames = new HashSet<>();
		List<KeyColumn> attributes = deduplicate(relationship.getAttributes(), usedNames);
		List<List<KeyColumn>> participantColumns = addParticipantColumns(ctx, table, participants, usedNames);
		addColumns(table, attributes);

		List<KeyColumn> holderColumns = participantColumns.get(participants.indexOf(holder));
		List<KeyColumn> referencedColumns = participantColumns.get(participants.indexOf(referenced));
		setPrimaryKey(ctx, table, relationship.getName(), Collections.singletonList(holderColumns));
		if (oneToOne && !referencedColumns.isEmpty()) {
			table.getUniqueKeys().add(names(referencedColumns));
		}
		addParticipantForeignKeys(ctx, table, participants, participantColumns);

		if (referenced.getCardinality() == CardinalityType.ONE) {
			table.getNotes().add(minimumNotEnforced(relationship, holder.getTarget(), referenced.getTarget()));
		}
		if (oneToOne && holder.getCardinality() == CardinalityType.ONE) {
			table.getNotes().add(minimumNotEnforced(relationship, referenced.getTarget(), holder.getTarget()));
		}
	}

	/**
	 * Maps a many-to-many, ternary or unspecified relationship to its own table.
	 * For ternary relationships, every participant with a maximum cardinality of 1
	 * is determined by the others, which yields a candidate key without its columns.
	 */
	private void buildRelationshipTable(BuildContext ctx, Relationship relationship, List<RelationEntity> participants) {
		Table table = ctx.schema.addTable(relationship.getName());
		Set<String> usedNames = new HashSet<>();
		List<KeyColumn> attributes = deduplicate(relationship.getAttributes(), usedNames);
		List<List<KeyColumn>> participantColumns = addParticipantColumns(ctx, table, participants, usedNames);
		addColumns(table, attributes);

		List<List<List<KeyColumn>>> candidateKeys = new ArrayList<>();
		if (participants.size() > 2) {
			for (int i = 0; i < participants.size(); i++) {
				if (isSingle(participants.get(i).getCardinality())) {
					List<List<KeyColumn>> candidate = new ArrayList<>(participantColumns);
					candidate.remove(i);
					candidateKeys.add(candidate);
				}
				if (participants.get(i).getCardinality() == CardinalityType.ONE) {
					table.getNotes().add(ternaryMinimumNotEnforced(relationship, participants, i));
				}
			}
		}
		if (candidateKeys.isEmpty()) {
			candidateKeys.add(participantColumns);
		}
		setPrimaryKey(ctx, table, relationship.getName(), candidateKeys.get(0));
		for (int i = 1; i < candidateKeys.size(); i++) {
			List<String> unique = new ArrayList<>();
			for (List<KeyColumn> part : candidateKeys.get(i)) {
				unique.addAll(names(part));
			}
			table.getUniqueKeys().add(unique);
		}
		addParticipantForeignKeys(ctx, table, participants, participantColumns);
	}

	private List<List<KeyColumn>> addParticipantColumns(BuildContext ctx, Table table, List<RelationEntity> participants, Set<String> usedNames) {
		List<List<KeyColumn>> participantColumns = new ArrayList<>();
		for (RelationEntity participant : participants) {
			Entity entity = participant.getTarget();
			List<KeyColumn> columns = entity == null ? List.of() : referencingColumns(participant, ctx.keyOf(entity), usedNames);
			participantColumns.add(columns);
			addColumns(table, columns);
		}
		return participantColumns;
	}

	private void addParticipantForeignKeys(BuildContext ctx, Table table, List<RelationEntity> participants, List<List<KeyColumn>> participantColumns) {
		for (int i = 0; i < participants.size(); i++) {
			Entity entity = participants.get(i).getTarget();
			if (entity != null) {
				List<KeyColumn> columns = participantColumns.get(i);
				addForeignKey(table, foreignKeyName(table, columns), columns, entity.getName(), ctx.keyOf(entity), ON_DELETE_CASCADE);
			}
		}
	}

	/** Note for a cardinality [1] on {@code required}: every {@code entity} needs a related {@code required}. */
	private String minimumNotEnforced(Relationship relationship, Entity entity, Entity required) {
		return relationship.getName() + ": each " + entity.getName() + " requires a related " + required.getName()
				+ " (cardinality [1] of " + required.getName() + "); this minimum is not enforced.";
	}

	private String ternaryMinimumNotEnforced(Relationship relationship, List<RelationEntity> participants, int index) {
		List<String> others = new ArrayList<>();
		for (int i = 0; i < participants.size(); i++) {
			if (i != index && participants.get(i).getTarget() != null) {
				others.add(participants.get(i).getTarget().getName());
			}
		}
		String required = participants.get(index).getTarget().getName();
		return relationship.getName() + ": each combination of " + String.join(" and ", others) + " requires a related " + required
				+ " (cardinality [1] of " + required + "); this minimum is not enforced.";
	}

	private static boolean isSingle(CardinalityType cardinality) {
		return cardinality == CardinalityType.ONE || cardinality == CardinalityType.ZERO_OR_ONE;
	}

	private static boolean isUnspecified(CardinalityType cardinality) {
		return cardinality == null || cardinality == CardinalityType.NONE;
	}

	/**
	 * Creates the columns that reference the given key, named {@code <role or entity>_<key>}.
	 * The prefix is skipped when the key column already starts with it (e.g. order_number of Order).
	 */
	private List<KeyColumn> referencingColumns(RelationEntity participant, List<KeyColumn> key, Set<String> usedNames) {
		String prefix = participant.getRole() != null && !participant.getRole().isBlank()
				? toIdentifier(participant.getRole())
				: toIdentifier(participant.getTarget().getName());
		List<KeyColumn> result = new ArrayList<>();
		for (KeyColumn column : key) {
			String baseName = column.name.toLowerCase(Locale.ROOT).startsWith(prefix + "_") ? column.name : prefix + "_" + column.name;
			String name = GeneratorUtils.findUniqueName(baseName, usedNames);
			result.add(new KeyColumn(name, column.attribute, name.equals(baseName) ? null : baseName));
		}
		return result;
	}

	/** Converts a name such as "OrderItem" or "line manager" to "order_item" or "line_manager". */
	static String toIdentifier(String name) {
		String snake = name.trim()
				.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
				.replaceAll("[^A-Za-z0-9_]+", "_");
		return snake.toLowerCase(Locale.ROOT);
	}

	private void addColumns(Table table, List<KeyColumn> columns) {
		for (KeyColumn column : columns) {
			addColumn(table, column);
		}
	}

	private Column addColumn(Table table, KeyColumn column) {
		if (column.attribute.getType() == AttributeType.DERIVED) {
			return null;
		}
		Column added = new Column(column.name, column.attribute, column.renamedFrom);
		table.getColumns().add(added);
		return added;
	}

	private void setPrimaryKey(BuildContext ctx, Table table, String keyOwner, List<List<KeyColumn>> parts) {
		List<KeyColumn> key = new ArrayList<>();
		for (List<KeyColumn> part : parts) {
			key.addAll(part);
		}
		table.getPrimaryKey().addAll(names(key));
		ctx.keys.put(keyOwner, key);
	}

	private String foreignKeyName(Table table, List<KeyColumn> columns) {
		return "fk_" + table.getName() + (columns.isEmpty() ? "" : "_" + columns.get(0).name);
	}

	private void addForeignKey(Table table, String name, List<KeyColumn> columns, String refTable, List<KeyColumn> refKey, String onDelete) {
		if (!columns.isEmpty()) {
			table.getForeignKeys().add(new ForeignKey(name, names(columns), refTable, names(refKey), onDelete));
		}
	}

	private static List<String> names(List<KeyColumn> columns) {
		List<String> names = new ArrayList<>();
		for (KeyColumn column : columns) {
			names.add(column.name);
		}
		return names;
	}

	private List<KeyColumn> filterByType(List<KeyColumn> columns, AttributeType type) {
		List<KeyColumn> result = new ArrayList<>();
		for (KeyColumn column : columns) {
			if (column.attribute.getType() == type) {
				result.add(column);
			}
		}
		return result;
	}

	private List<KeyColumn> deduplicate(final List<Attribute> attributes, Set<String> usedNames) {
		List<KeyColumn> result = new ArrayList<>();
		if (attributes != null) {
			for (Attribute a : attributes) {
				String name = GeneratorUtils.findUniqueName(a.getName(), usedNames);
				result.add(new KeyColumn(name, a, name.equals(a.getName()) ? null : a.getName()));
			}
		}
		return result;
	}

	private List<RelationEntity> getParticipants(Relationship relationship) {
		List<RelationEntity> participants = new ArrayList<>();
		for (RelationEntity participant : new RelationEntity[] {relationship.getFirst(), relationship.getSecond(), relationship.getThird()}) {
			if (participant != null) {
				participants.add(participant);
			}
		}
		return participants;
	}

	private RelationEntity getStrongParticipant(final Relationship r) {
		if (r.getFirst().getTarget().isWeak()) {
			return r.getSecond();
		} else {
			return r.getFirst();
		}
	}

	private Entity getWeakEntity(final Relationship r) {
		if (r.getFirst().getTarget().isWeak()) {
			return r.getFirst().getTarget();
		} else {
			return r.getSecond().getTarget();
		}
	}

	// ---------------------------------------------------------------------
	// Step 2: render the relational schema as SQL
	// Tables are created first, foreign keys are added afterwards with
	// ALTER TABLE, so that table order and reference cycles do not matter.
	// ---------------------------------------------------------------------

	private StringConcatenation renderCreate(SqlSchema schema) {
		StringConcatenation content = new StringConcatenation();
		for (Table table : schema.getTables()) {
			renderTable(content, table);
		}
		for (Table table : schema.getTables()) {
			for (ForeignKey foreignKey : table.getForeignKeys()) {
				content.append("ALTER TABLE ");
				content.append(table.getName());
				content.append(" ADD CONSTRAINT ");
				content.append(foreignKey.getName());
				content.append(" ");
				content.append(renderForeignKeyClause(foreignKey));
				content.append(";");
				content.newLineIfNotEmpty();
			}
		}
		return content;
	}

	private StringConcatenation renderDrop(SqlSchema schema) {
		StringConcatenation content = new StringConcatenation();
		List<Table> tables = schema.getTables();
		Collections.reverse(tables);
		for (Table table : tables) {
			for (ForeignKey foreignKey : table.getForeignKeys()) {
				content.append("ALTER TABLE ");
				content.append(table.getName());
				content.append(" ");
				content.append(dropForeignKeyKeyword());
				content.append(" ");
				content.append(foreignKey.getName());
				content.append(";");
				content.newLineIfNotEmpty();
			}
		}
		for (Table table : tables) {
			content.append("DROP TABLE ");
			content.append(table.getName());
			content.append(";");
			content.newLineIfNotEmpty();
		}
		return content;
	}

	/** Statement part to remove a foreign key constraint, e.g. "DROP CONSTRAINT". */
	protected String dropForeignKeyKeyword() {
		return "DROP CONSTRAINT";
	}

	private void renderTable(StringConcatenation content, Table table) {
		for (String note : table.getNotes()) {
			content.append("-- Note: ");
			content.append(note);
			content.newLineIfNotEmpty();
		}
		content.append("CREATE TABLE ");
		content.append(table.getName());
		content.append(" (");
		content.newLineIfNotEmpty();

		// each entry is one line inside the CREATE TABLE statement: {definition, comment}
		List<String[]> lines = new ArrayList<>();
		for (Column column : table.getColumns()) {
			StringBuilder comment = new StringBuilder();
			String definition = column.getName() + renderColumnType(column, comment);
			lines.add(new String[] {definition, comment.toString()});
		}
		if (!table.getPrimaryKey().isEmpty()) {
			lines.add(new String[] {"PRIMARY KEY (" + String.join(", ", table.getPrimaryKey()) + ")", ""});
		}
		for (List<String> unique : table.getUniqueKeys()) {
			lines.add(new String[] {"UNIQUE (" + String.join(", ", unique) + ")", ""});
		}

		for (int i = 0; i < lines.size(); i++) {
			content.append("\t");
			content.append(lines.get(i)[0]);
			if (i < lines.size() - 1) {
				content.append(",");
			}
			if (!lines.get(i)[1].isEmpty()) {
				content.append("\t-- ");
				content.append(lines.get(i)[1]);
			}
			content.newLineIfNotEmpty();
		}
		content.append(");");
		content.newLineIfNotEmpty();
	}

	private String renderForeignKeyClause(ForeignKey foreignKey) {
		StringBuilder sb = new StringBuilder();
		sb.append("FOREIGN KEY (");
		sb.append(String.join(", ", foreignKey.getColumns()));
		sb.append(") REFERENCES ");
		sb.append(foreignKey.getRefTable());
		sb.append(" (");
		sb.append(String.join(", ", foreignKey.getRefColumns()));
		sb.append(")");
		if (foreignKey.getOnDelete() != null) {
			sb.append(" ON DELETE ");
			sb.append(foreignKey.getOnDelete());
		}
		return sb.toString();
	}

	/** Renders the data type (with leading space) and collects explanations in {@code comment}. */
	private String renderColumnType(Column column, StringBuilder comment) {
		Attribute attribute = column.getAttribute();
		if (column.getRenamedFrom() != null) {
			addComment(comment, "renamed from: " + column.getRenamedFrom());
		}
		String originalType;
		int size;
		Integer precision = null;
		if (attribute.getDatatype() != null) {
			originalType = attribute.getDatatype().getType();
			size = attribute.getDatatype().getSize();
			precision = attribute.getDatatype().getD();
		} else {
			originalType = "VARCHAR";
			size = 255;
			addComment(comment, "added default type");
		}
		originalType = replaceUnderscores(originalType);
		String mappedType = this.mapDataType(originalType);
		if (mappedType == null) {
			mappedType = originalType;
			addComment(comment, "unknown type");
		} else if (!mappedType.equals(originalType)) {
			addComment(comment, "type mapped from: " + originalType);
		}
		boolean nullable = column.getNullable() != null ? column.getNullable() : attribute.getType() == AttributeType.OPTIONAL;
		String transformedDataType = this.transformDataType(attribute, mappedType, size, precision, nullable, comment);
		if (transformedDataType != null && !transformedDataType.isEmpty()) {
			return " " + transformedDataType;
		}
		return "";
	}

	protected String transformDataType(Attribute attribute, String mappedType, int size, Integer precision, StringBuilder comment) {
		return transformDataType(attribute, mappedType, size, precision, attribute.getType() == AttributeType.OPTIONAL, comment);
	}

	protected String transformDataType(Attribute attribute, String mappedType, int size, Integer precision, boolean nullable, StringBuilder comment) {
		if (size > 0) {
			String strPrecision = "";
			if (precision != null && precision > 0) {
				strPrecision = ", " + precision;
			}
			mappedType = mappedType + "(" + size + strPrecision + ")";
		}
		if (!nullable) {
			mappedType = mappedType + " " + GeneratorUtils.NOT_NULL;
		}
		return mappedType;
	}

	protected String mapDataType(String type) {
		return type;
	}

	// ER model does not support spaces in data types
	private String replaceUnderscores(String value) {
		return value.replace("_", " ");
	}

	protected void addComment(StringBuilder comment, String str) {
		if (comment.length() > 0 && !str.isEmpty()) {
			comment.append("; ");
		}
		comment.append(str);
	}
}
