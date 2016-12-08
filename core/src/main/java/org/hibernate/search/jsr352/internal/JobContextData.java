/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.criterion.Criterion;
import org.hibernate.search.jsr352.internal.util.PartitionBound;

/**
 * Container for data shared across the entire batch job.
 *
 * @author Gunnar Morling
 * @author Mincong Huang
 */
public class JobContextData implements Serializable {

	private static final long serialVersionUID = 4465274690302894983L;

	/**
	 * The map of key value pair (string, class-type), designed for storage of name and class type of all root entities.
	 * In JSR 352 standard, only string values can be propagated using job properties, but class types are frequently
	 * used too. So this map facilitates this kind of lookup.
	 */
	private Map<String, Class<?>> entityTypeMap;

	/**
	 * The total number of entities to index over all the entity types.
	 */
	private long totalEntityToIndex;

	/**
	 * The list of partition boundaries, one element per partition
	 */
	private List<PartitionBound> partitionBounds;

	private Set<Criterion> criterions;

	public JobContextData() {
		entityTypeMap = new HashMap<>();
	}

	public void setEntityTypeSet(Set<Class<?>> entityTypes) {
		entityTypes.forEach( clz -> entityTypeMap.put( clz.getName(), clz ) );
	}

	public Set<String> getEntityNameSet() {
		return entityTypeMap.keySet();
	}

	public Set<Class<?>> getEntityTypeSet() {
		return new HashSet<Class<?>>( entityTypeMap.values() );
	}

	public String[] getEntityNameArray() {
		Set<String> keySet = entityTypeMap.keySet();
		return keySet.toArray( new String[keySet.size()] );
	}

	public Class<?> getIndexedType(String entityName) throws ClassNotFoundException {
		Class<?> entityType = entityTypeMap.get( entityName );
		if ( entityType == null ) {
			String msg = String.format( "entityName %s not found.", entityName );
			throw new ClassNotFoundException( msg );
		}
		return entityType;
	}

	public long getTotalEntityToIndex() {
		return totalEntityToIndex;
	}

	public Set<Criterion> getCriterions() {
		return criterions;
	}

	public void setTotalEntityToIndex(long totalEntityToIndex) {
		this.totalEntityToIndex = totalEntityToIndex;
	}

	/**
	 * Increment to total entity number to index
	 *
	 * @param increment the entity number to index for one entity type
	 */
	public void incrementTotalEntity(long increment) {
		totalEntityToIndex += increment;
	}

	public void setPartitionBounds(List<PartitionBound> partitionBounds) {
		this.partitionBounds = partitionBounds;
	}

	public PartitionBound getPartitionBound(int partitionID) {
		return partitionBounds.get( partitionID );
	}

	public void setCriterions(Set<Criterion> criterions) {
		this.criterions = criterions;
	}

	@Override
	public String toString() {
		return "JobContextData [entityTypeMap=" + entityTypeMap + ", totalEntityToIndex=" + totalEntityToIndex + ", partitionBounds=" + partitionBounds
				+ ", criterions=" + criterions + "]";
	}
}
