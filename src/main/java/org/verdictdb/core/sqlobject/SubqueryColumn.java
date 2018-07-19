package org.verdictdb.core.sqlobject;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Subquery that may appear in the where clause.
 */
public class SubqueryColumn implements UnnamedColumn {
  
  private static final long serialVersionUID = 4046157399674779659L;
  
  SelectQuery subquery = new SelectQuery();

  public SubqueryColumn() {
  }

  public SubqueryColumn(SelectQuery relation) {
    subquery = relation;
  }

  public void setSubquery(SelectQuery relation) {
    subquery = relation;
  }

  public static SubqueryColumn getSubqueryColumn(SelectQuery relation) {
    return new SubqueryColumn(relation);
  }

  public SelectQuery getSubquery() {
    return subquery;
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return EqualsBuilder.reflectionEquals(this, obj);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  @Override
  public boolean isAggregateColumn() {
    return false;
  }

  // not need this
  @Override
  public SubqueryColumn deepcopy() {
    return this;
  }
  
}