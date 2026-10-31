package org.big.erd.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.big.erd.entityRelationship.Model;
import org.big.erd.generator.sql.MySqlGenerator;
import org.big.erd.generator.sql.SqlGenerator;
import org.big.erd.generator.sql.SqlImport;
import org.big.erd.generator.sql.SqlSchema;
import org.big.erd.generator.sql.SqlSchema.Column;
import org.big.erd.generator.sql.SqlSchema.ForeignKey;
import org.big.erd.generator.sql.SqlSchema.Table;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.inject.Inject;

/**
 * Tests how relationship cardinalities are mapped to SQL constraints.
 */
@ExtendWith(InjectionExtension.class)
@InjectWith(EntityRelationshipInjectorProvider.class)
public class SqlCardinalityTest {

	private static final String ENTITIES = "erdiagram Test\n"
			+ "entity Customer { id: int key  name: string }\n"
			+ "entity Order { order_number: int key  price: double }\n"
			+ "entity Passport { nr: int key }\n"
			+ "entity Product { sku: int key }\n";

	@Inject
	private ParseHelper<Model> parseHelper;

	private SqlSchema schema(String relationships) throws Exception {
		return new SqlGenerator().buildSchema(parse(relationships));
	}

	private Model parse(String relationships) throws Exception {
		Model model = parseHelper.parse(ENTITIES + relationships);
		assertTrue(model.eResource().getErrors().isEmpty(), model.eResource().getErrors().toString());
		return model;
	}

	private static Column column(Table table, String name) {
		for (Column column : table.getColumns()) {
			if (column.getName().equals(name)) {
				return column;
			}
		}
		return null;
	}

	private static ForeignKey foreignKey(Table table, String name) {
		for (ForeignKey foreignKey : table.getForeignKeys()) {
			if (foreignKey.getName().equals(name)) {
				return foreignKey;
			}
		}
		return null;
	}

	@Test
	public void oneToManyAddsMandatoryForeignKeyToManySide() throws Exception {
		SqlSchema schema = schema("relationship Places { Customer[1] -> Order[N] }");
		assertNull(schema.getTable("Places"), "1:N without attributes needs no own table");

		Table order = schema.getTable("Order");
		Column customerId = column(order, "customer_id");
		assertNotNull(customerId);
		assertEquals(Boolean.FALSE, customerId.getNullable());
		assertTrue(order.getUniqueKeys().isEmpty());

		ForeignKey fk = foreignKey(order, "fk_Places");
		assertEquals(List.of("customer_id"), fk.getColumns());
		assertEquals("Customer", fk.getRefTable());
		assertEquals(List.of("id"), fk.getRefColumns());
		assertNull(fk.getOnDelete(), "[1] must block deleting a referenced customer");
		assertTrue(schema.getTable("Customer").getForeignKeys().isEmpty());
	}

	@Test
	public void manySideCanBeWrittenFirst() throws Exception {
		SqlSchema schema = schema("relationship Places { Order[N] -> Customer[1] }");
		assertNotNull(column(schema.getTable("Order"), "customer_id"));
		assertNull(column(schema.getTable("Customer"), "order_number"));
	}

	@Test
	public void optionalOneToManyIsNullableWithSetNull() throws Exception {
		SqlSchema schema = schema("relationship Places { Customer[0..1] -> Order[0..N] }");
		Table order = schema.getTable("Order");
		assertEquals(Boolean.TRUE, column(order, "customer_id").getNullable());
		assertEquals("SET NULL", foreignKey(order, "fk_Places").getOnDelete());
	}

	@Test
	public void oneToOneAddsUniqueForeignKeyOnSideWhosePartnerIsOne() throws Exception {
		SqlSchema schema = schema("relationship Owns { Customer[0..1] -> Passport[1] }");
		Table customer = schema.getTable("Customer");
		assertEquals(Boolean.FALSE, column(customer, "passport_nr").getNullable());
		assertEquals(List.of(List.of("passport_nr")), customer.getUniqueKeys());
		assertTrue(customer.getNotes().isEmpty());
		assertTrue(schema.getTable("Passport").getForeignKeys().isEmpty());
	}

	@Test
	public void mandatoryOneToOneReportsUnenforcedMinimum() throws Exception {
		SqlSchema schema = schema("relationship Owns { Customer[1] -> Passport[1] }");
		Table passport = schema.getTable("Passport");
		assertEquals(Boolean.FALSE, column(passport, "customer_id").getNullable());
		assertEquals(List.of(List.of("customer_id")), passport.getUniqueKeys());
		assertEquals(1, passport.getNotes().size());
		assertTrue(passport.getNotes().get(0).contains("each Customer requires a related Passport"));
	}

	@Test
	public void oneToManyWithAttributesKeepsOwnTable() throws Exception {
		SqlSchema schema = schema("relationship Places { Customer[1] -> Order[N]  placed_at: date }");
		Table places = schema.getTable("Places");
		assertNotNull(places);
		assertEquals(List.of("customer_id", "order_number", "placed_at"), places.getColumnNames());
		assertEquals(List.of("order_number"), places.getPrimaryKey(), "every order appears at most once");
		assertEquals(1, places.getNotes().size());
		assertTrue(places.getNotes().get(0).contains("each Order requires a related Customer"));
		assertNull(column(schema.getTable("Order"), "customer_id"));
	}

	@Test
	public void manyToManyAndUnspecifiedKeepJunctionTable() throws Exception {
		SqlSchema schema = schema("relationship Buys { Customer[N] -> Product[N] }\nrelationship Likes { Customer -> Product }");
		for (String name : List.of("Buys", "Likes")) {
			Table table = schema.getTable(name);
			assertEquals(List.of("customer_id", "product_sku"), table.getPrimaryKey());
			assertEquals(2, table.getForeignKeys().size());
			for (ForeignKey fk : table.getForeignKeys()) {
				assertEquals("CASCADE", fk.getOnDelete());
			}
		}
	}

	@Test
	public void ternaryUsesCandidateKeysFromSingleParticipants() throws Exception {
		SqlSchema schema = schema("relationship Sale { Customer[1] -> Order[0..1] -> Product[N] }");
		Table sale = schema.getTable("Sale");
		assertEquals(List.of("order_number", "product_sku"), sale.getPrimaryKey());
		assertEquals(List.of(List.of("customer_id", "product_sku")), sale.getUniqueKeys());
		assertEquals(1, sale.getNotes().size());
	}

	@Test
	public void roleNamesSelfReferenceColumns() throws Exception {
		SqlSchema schema = schema("entity Employee { id: int key }\n"
				+ "relationship Manages { Employee[1 | \"manager\"] -> Employee[N | \"report\"] }");
		Table employee = schema.getTable("Employee");
		assertNotNull(column(employee, "manager_id"));
		assertEquals("Employee", foreignKey(employee, "fk_Manages").getRefTable());
	}

	@Test
	public void foreignKeysAreAddedAfterAllTables() throws Exception {
		String sql = new SqlGenerator().generate(parse("relationship Places { Order[N] -> Customer[1] }"));
		int lastCreate = sql.lastIndexOf("CREATE TABLE");
		int alter = sql.indexOf("ALTER TABLE Order ADD CONSTRAINT fk_Places FOREIGN KEY (customer_id) REFERENCES Customer (id);");
		assertTrue(alter > lastCreate, sql);
		assertFalse(sql.contains("ON DELETE"), sql);
	}

	@Test
	public void dropRemovesConstraintsBeforeTables() throws Exception {
		Model model = parse("relationship Places { Customer[1] -> Order[N] }");
		String sql = new SqlGenerator().generateDrop(model);
		assertTrue(sql.indexOf("ALTER TABLE Order DROP CONSTRAINT fk_Places;") < sql.indexOf("DROP TABLE"), sql);
		String mysql = new MySqlGenerator().generateDrop(model);
		assertTrue(mysql.contains("ALTER TABLE Order DROP FOREIGN KEY fk_Places;"), mysql);
	}

	@Test
	public void importRestoresCardinalities() throws Exception {
		String sql = new SqlGenerator().generate(parse(
				"relationship Places { Customer[1] -> Order[N] }\n"
				+ "relationship Owns { Customer[0..1] -> Passport[0..1] }\n"
				+ "relationship Buys { Customer[1] -> Product[N]  quantity: int }"));
		// the import must also accept Unix line endings
		String erd = new SqlImport().importSql("Test", sql.replace("\r\n", "\n"));
		assertTrue(erd.contains("relationship Places {\n\tCustomer[1] -> Order[N]\n}") || erd.contains("relationship Places {\r\n\tCustomer[1] -> Order[N]\r\n}"), erd);
		assertTrue(erd.contains("Customer[0..1] -> Passport[0..1]"), erd);
		assertTrue(erd.contains("Customer[1] -> Product[N]"), erd);
		assertTrue(erd.contains("quantity: int"), erd);
		assertFalse(erd.contains("customer_id"), "foreign key columns are not entity attributes:\n" + erd);
		assertTrue(parseHelper.parse(erd).eResource().getErrors().isEmpty(), "imported model must parse:\n" + erd);
	}
}
