package edu.cmu.cs.able.typelib.comp;

import incubator.pval.Ensure;
import edu.cmu.cs.able.typelib.prim.AnyType;
import edu.cmu.cs.able.typelib.prim.PrimitiveScope;
import edu.cmu.cs.able.typelib.scope.AmbiguousNameException;
import edu.cmu.cs.able.typelib.type.DataType;
import edu.cmu.cs.able.typelib.type.DataTypeScope;

/**
 * Data type implementing a set of another data type.
 */
public class SetDataType extends CollectionDataType {
	/**
	 * Prefix for set name.
	 */
	private static final String SET_NAME_PREFIX = "set<";
	
	/**
	 * Suffix for set name.
	 */
	private static final String SET_NAME_SUFFIX = ">";
	
	/**
	 * Creates a new set.
	 * @param inner_type the set's inner type
	 * @param any the any type
	 */
	public SetDataType(DataType inner_type, AnyType any) {
		super(build_set_name(inner_type), inner_type, any);
	}
	
	/**
	 * Obtains the name of a set over an inner type.
	 * @param inner the inner type
	 * @return the name of the data type
	 */
	public static final String build_set_name(DataType inner) {
		Ensure.not_null(inner, "inner == null");
		return SET_NAME_PREFIX + inner.name() + SET_NAME_SUFFIX;
	}
	
	/**
	 * Obtains (and creates, if necessary) the set data type that contains the
	 * given inner data type. The set data type is created, if necessary, in
	 * the same scope as the inner type.
	 * @param inner the inner data type
	 * @param pscope the primitive data type scope which should be the scope
	 * (or a parent of the scope) of inner
	 * @return the set data type
	 */
	public static SetDataType set_of(DataType inner, PrimitiveScope pscope) {
		Ensure.not_null(inner);
		Ensure.not_null(pscope);
		Ensure.is_true(inner.in_scope(pscope));
		
		DataTypeScope scope = inner.parent_dts();
		DataType found = null;
		try {
			found = scope.find(build_set_name(inner));
		} catch (AmbiguousNameException e) {
			/*
			 * No set found.
			 */
		}
		
		if (found == null) {
			found = new SetDataType(inner, pscope.any());
			scope.add(found);
		}
		
		Ensure.not_null(found);
		Ensure.is_instance(found, SetDataType.class);
		return (SetDataType) found;
	}
	
	@Override
	public SetDataValue make() {
		return new SetDataValue(this);
	}

	@Override
	public boolean is_abstract() {
		return false;
	}
}
