/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Container for data shared across the entire batch job.
 *
 * @author Gunnar Morling
 * @author Mincong Huang
 */
public class JobContextData {

	/**
	 * The map of key value pair (string, class-type), designed for storage of
	 * name and class type of all root entities. In JSR 352 standard, only
	 * string values can be propagated using job properties, but class types are
	 * frequently used too. So this map facilites this kind of lookup.
	 */
	private Map<String, Class<?>> entityClazzMap;

	/**
	 * The map of key value pair (string, long), designed for storage of name
	 * and number of rows to index of all root entities.
	 */
	private Map<String, Long> entityCountMap = new HashMap<>();

	/**
	 * The total number of entities to index over all the entity types.
	 */
	private long totalEntityToIndex;

	/**
	 * The array of first ID
	 */
	private Object[] firstIDArray;

	/**
	 * The array of last ID
	 */
	private Object[] lastIDArray;

	public JobContextData(Set<Class<?>> entityClazzes) {
		entityClazzMap = new HashMap<>();
		entityClazzes.forEach( clz -> entityClazzMap.put( clz.toString(), clz ) );
	}

	public Set<String> getEntityNameSet() {
		return entityClazzMap.keySet();
	}

	public Set<Class<?>> getEntityClazzSet() {
		return new HashSet<Class<?>>( entityClazzMap.values() );
	}

	public String[] getEntityNameArray() {
		Set<String> keySet = entityClazzMap.keySet();
		return keySet.toArray( new String[keySet.size()] );
	}

	public Class<?> getIndexedType(String entityName) throws ClassNotFoundException {
		Class<?> clazz = entityClazzMap.get( entityName );
		if ( clazz == null ) {
			String msg = String.format( "entityName %s not found.", entityName );
			throw new ClassNotFoundException( msg );
		}
		return clazz;
	}

	public long getTotalEntityToIndex() {
		return totalEntityToIndex;
	}

	public void setTotalEntityToIndex(long totalEntityToIndex) {
		this.totalEntityToIndex = totalEntityToIndex;
	}

	/**
	 * Increment to total entity number to index
	 *
	 * @param increment the entity number to index for one entity type
	 */
	public void incrementTotalEntity( long increment ) {
		totalEntityToIndex += increment;
	}

	public Object[] getFirstIDArray() {
		return firstIDArray;
	}

	public Object getFirstID(int index) {
		return firstIDArray[index];
	}

	public void setFirstIDArray(Object[] firstIDArray) {
		this.firstIDArray = firstIDArray;
	}

	public Object[] getLastIDArray() {
		return lastIDArray;
	}

	public Object getLastID(int index) {
		return lastIDArray[index];
	}

	public void setLastIDArray(Object[] lastIDArray) {
		this.lastIDArray = lastIDArray;
	}

	public long getRowsToIndex(String entityName) {
		return entityCountMap.get( entityName );
	}

	public void setRowsToIndex(String entityName, long rowsToIndex) {
		entityCountMap.put( entityName, rowsToIndex );
	}

	@Override
	public String toString() {
		return "JobContextData [entityClazzMap=" + entityClazzMap + ", entityCountMap="
				+ entityCountMap + ", totalEntityToIndex=" + totalEntityToIndex
				+ ", firstIDArray=" + Arrays.toString( firstIDArray ) + ", lastIDArray="
				+ Arrays.toString( lastIDArray ) + "]";
	}
}
