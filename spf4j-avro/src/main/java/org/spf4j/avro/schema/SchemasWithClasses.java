
package org.spf4j.avro.schema;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.Schema;

/**
 *
 * @author zoly
 */
//CHECKSTYLE IGNORE MissingSwitchDefault FOR NEXT 200 LINES
public final class SchemasWithClasses implements SchemaVisitor<Map<String, Schema>> {

  private final Map<String, Schema> schemas = new HashMap<>();

  @Override
  @SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
  public SchemaVisitorAction visitTerminal(final Schema schema) {
    switch (schema.getType()) {
      case FIXED:
      case ENUM:
        schemas.put(Schemas.getJavaClassName(schema), schema);
        break;
    }
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public SchemaVisitorAction visitNonTerminal(final Schema schema) {
    if (schema.getType() == Schema.Type.RECORD) {
      schemas.put(Schemas.getJavaClassName(schema), schema);
    }
    return SchemaVisitorAction.CONTINUE;
  }

  @Override
  public SchemaVisitorAction afterVisitNonTerminal(final Schema terminal) {
    return SchemaVisitorAction.CONTINUE;
  }

  public Map<String, Schema> get() {
    return schemas;
  }

  @Override
  public String toString() {
    return "SchemasWithClasses{" + "schemas=" + schemas + '}';
  }


}
